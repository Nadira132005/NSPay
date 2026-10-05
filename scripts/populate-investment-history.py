#!/usr/bin/env python3
"""Upsert older Alice trades and daily profit snapshots into the configured local database."""
import argparse
import importlib.util
import json
from decimal import Decimal, ROUND_HALF_UP
from datetime import datetime, timedelta, timezone
from pathlib import Path
import subprocess
import tempfile
from urllib import request

spec = importlib.util.spec_from_file_location('setup_db', Path(__file__).with_name('setup-db.py'))
setup = importlib.util.module_from_spec(spec)
spec.loader.exec_module(setup)
CENT = Decimal('.01')

def money(value):
    return value.quantize(CENT, rounding=ROUND_HALF_UP)

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--env-file', type=Path, default=setup.ROOT / '.env')
    parser.add_argument('--base-url', default='http://127.0.0.1:8082')
    args = parser.parse_args()
    settings = setup.settings(args.env_file)
    def api(path, body=None, token=None):
        headers = {'Content-Type': 'application/json'}
        if token: headers['Authorization'] = 'Bearer ' + token
        req = request.Request(args.base_url + path, data=None if body is None else json.dumps(body).encode(), headers=headers)
        with request.urlopen(req, timeout=35) as response: return json.load(response)
    login = api('/api/login/', {'name': 'alice', 'password': 'DemoPay123!'})
    token = login['token']
    quotes = api('/api/investments/quotes', token=token)
    source = quotes['AAPL']['marketProvider']
    if not quotes['AAPL'].get('synthetic'):
        raise ValueError('Historical sample population requires synthetic CuckooTrade prices')
    with tempfile.TemporaryDirectory(prefix='nspay-history-') as folder:
        config = Path(folder) / 'client.cnf'
        config.write_text('[client]\nuser='+setup.option_value(settings['DB_USERNAME'])+'\npassword='+setup.option_value(settings['DB_PASSWORD'])+'\n')
        config.chmod(0o600)
        command = ['mysql', '--defaults-file='+str(config), '--host='+settings['DB_HOST'], '--port='+settings['DB_PORT'], '--protocol=TCP', '--batch', '--skip-column-names', settings['DB_NAME']]
        def sql(statement):
            result = subprocess.run(command, input=statement, text=True, capture_output=True)
            if result.returncode: raise ValueError('Database statement failed; no credentials were logged')
            return [line.split('\t') for line in result.stdout.splitlines() if line]
        user_id,account_id = map(int,sql("SELECT id,accountId FROM USERS WHERE name='alice';")[0])
        if sql("SELECT id FROM INVESTMENT_COMMANDS WHERE userId="+str(user_id)+" AND requestKey='sample-history-v1';"):
            print('Alice: older trades and profit history already exist; records preserved')
            return 0
        # Ensure existing position migration is complete before replacing its projection.
        api('/api/investments', token=token)
        wallet_id,balance,wallet_version = sql('SELECT id,balance,version FROM INVESTMENT_WALLETS WHERE userId='+str(user_id)+';')[0]
        account_status = sql('SELECT status FROM ACCOUNTS WHERE id='+str(account_id)+';')[0][0]
        if account_status != 'ACTIVE': raise ValueError('Alice is restricted; account preserved')
        existing = sql('SELECT id,symbol,side,quantity,total,createdAt,provider FROM TRADE_ORDERS WHERE userId='+str(user_id)+" AND status='FILLED' ORDER BY createdAt,id;")
        api_dates={};page=0
        while True:
            data=api('/api/investments/orders?page='+str(page)+'&size=100',token=token)
            for row in data['content']:api_dates[row['id']]=datetime.fromisoformat(row['createdAt'].replace('Z','+00:00'))
            if page+1>=data['totalPages']:break
            page+=1
        db_offset=(datetime.fromisoformat(existing[0][5])-api_dates[int(existing[0][0])].replace(tzinfo=None)) if existing else datetime.now().astimezone().utcoffset()
        def database_time(when):return (when.astimezone(timezone.utc).replace(tzinfo=None)+db_offset).strftime('%Y-%m-%d %H:%M:%S')
        symbols = {'AAPL','MSFT'} | {r[1] for r in existing}
        if any(r[6] != source for r in existing): raise ValueError('Existing holdings use another dataset; records preserved')
        histories = {}
        for symbol in sorted(symbols):
            bars = api('/api/investments/'+symbol+'/chart?range=1Y', token=token)['values']
            histories[symbol] = sorted((datetime.fromisoformat(b['datetime'].replace('Z','+00:00')).date(), Decimal(b['close'])) for b in bars)
        now = datetime.now(timezone.utc)
        plans = [('AAPL','BUY',Decimal('.5'),28),('MSFT','BUY',Decimal('.25'),21),('AAPL','SELL',Decimal('.1'),10)]
        new_orders=[]
        for symbol,side,quantity,days in plans:
            target=(now-timedelta(days=days)).date()
            day,price=max((d,p) for d,p in histories[symbol] if d <= target)
            when=datetime.combine(day,datetime.min.time(),timezone.utc)+timedelta(hours=20)
            fx=Decimal(str(quotes[symbol]['exchangeRate']))
            total=money(price*fx*quantity)
            new_orders.append([0,symbol,side,quantity,total,when,source,price,fx])
        orders=[[int(r[0]),r[1],r[2],Decimal(r[3]),Decimal(r[4]),api_dates[int(r[0])].astimezone(timezone.utc),r[6]] for r in existing]+new_orders
        orders.sort(key=lambda r:(r[5],r[0]))
        positions={}
        def apply(order):
            _,symbol,side,quantity,total,*_=order
            p=positions.setdefault(symbol,[Decimal(0),Decimal(0),Decimal(0)])
            if side=='BUY':p[0]+=quantity;p[1]+=total
            else:
                if quantity>p[0]:raise ValueError('Historical sale would oversell; no records changed')
                removed=p[1] if quantity==p[0] else (p[1]*quantity/p[0]).quantize(Decimal('.00000001'),rounding=ROUND_HALF_UP)
                p[0]-=quantity;p[1]-=removed;p[2]+=total-removed
        snapshots=[];cursor=0;day=orders[0][5].date()
        while day <= now.date():
            while cursor<len(orders) and orders[cursor][5].date()<=day:apply(orders[cursor]);cursor+=1
            value=Decimal(0);basis=Decimal(0);realized=sum((p[2] for p in positions.values()),Decimal(0))
            for symbol,(quantity,cost,_) in positions.items():
                if not quantity:continue
                candidates=[p for d,p in histories[symbol] if d<=day]
                if not candidates:raise ValueError('Missing historical price; no records changed')
                value+=quantity*candidates[-1]*Decimal(str(quotes[symbol]['exchangeRate']));basis+=cost
            snapshots.append((day,money(realized+value-basis),money(realized),money(value-basis),money(value)))
            day+=timedelta(days=1)
        if cursor != len(orders):raise ValueError('Orders extend beyond the valuation date; records preserved')
        net_cost=sum((o[4] if o[2]=='BUY' else -o[4] for o in new_orders),Decimal(0))
        funding=money(max(Decimal(0),net_cost+Decimal(100)-Decimal(balance)))
        literal=setup.sql_literal
        statements=['START TRANSACTION;', 'SELECT id FROM ACCOUNTS WHERE id='+str(account_id)+' FOR UPDATE;',
            'SELECT id FROM INVESTMENT_WALLETS WHERE id='+wallet_id+' FOR UPDATE;',
            'CREATE TEMPORARY TABLE seed_guard (id INT PRIMARY KEY);', 'INSERT INTO seed_guard VALUES(1);',
            # Duplicate-key aborts the connection/transaction when state changed since calculation.
            "INSERT INTO seed_guard SELECT 1 WHERE EXISTS(SELECT 1 FROM INVESTMENT_COMMANDS WHERE userId="+str(user_id)+" AND requestKey='sample-history-v1') OR NOT EXISTS(SELECT 1 FROM INVESTMENT_WALLETS WHERE id="+wallet_id+' AND version='+wallet_version+') OR NOT EXISTS(SELECT 1 FROM ACCOUNTS WHERE id='+str(account_id)+" AND status='ACTIVE' AND amount >= "+str(funding)+');',
            'UPDATE ACCOUNTS SET amount=amount-'+str(funding)+' WHERE id='+str(account_id)+';',
            'UPDATE INVESTMENT_WALLETS SET balance=balance+'+str(funding-net_cost)+',version=version+1 WHERE id='+wallet_id+';']
        if funding:
            statements.append("INSERT INTO TRANSACTIONS(cardNumber,amount,returnCode,timestamp,transactionType,counterpartyIban) SELECT cardNumber,"+str(funding)+",'00',NOW(),'INVESTMENT_DEPOSIT','Investment cash' FROM ACCOUNTS WHERE id="+str(account_id)+';')
        for o in new_orders:
            _,symbol,side,quantity,total,when,_,price,fx=o
            statements.append('INSERT INTO TRADE_ORDERS(userId,createdAt,version,symbol,side,quantity,price,total,status,provider,marketPrice,exchangeRate,priceAt,exchangeRateAt,quoteId) VALUES('+','.join([str(user_id),literal(database_time(when)),'0',literal(symbol),literal(side),str(quantity),str((price*fx).quantize(Decimal('.000001'))),str(total),"'FILLED'",literal(source),str(price),str(fx),literal(database_time(when)),literal(database_time(when)),"'sample-history-v1'"])+');')
        for symbol,(quantity,cost,realized) in positions.items():
            statements.append('INSERT INTO INVESTMENT_POSITIONS(userId,createdAt,version,provider,symbol,quantity,costBasis,realizedGain) VALUES('+','.join([str(user_id),'NOW()','0',literal(source),literal(symbol),str(quantity),str(cost),str(realized)])+') ON DUPLICATE KEY UPDATE quantity=VALUES(quantity),costBasis=VALUES(costBasis),realizedGain=VALUES(realizedGain),version=version+1;')
        for day,gain,realized,unrealized,value in snapshots:
            statements.append('INSERT INTO PORTFOLIO_PERFORMANCE(userId,createdAt,version,day,totalGain,realizedGain,unrealizedGain,holdingsValue,walletVersion) VALUES('+','.join([str(user_id),'NOW()','0',literal(str(day)),str(gain),str(realized),str(unrealized),str(value),str(int(wallet_version)+1)])+') ON DUPLICATE KEY UPDATE totalGain=VALUES(totalGain),realizedGain=VALUES(realizedGain),unrealizedGain=VALUES(unrealizedGain),holdingsValue=VALUES(holdingsValue),walletVersion=VALUES(walletVersion),version=version+1;')
        statements.extend(["INSERT INTO INVESTMENT_COMMANDS(userId,createdAt,version,requestKey,fingerprint,orderId) VALUES("+str(user_id)+",NOW(),0,'sample-history-v1','Historical sample population',NULL);",'COMMIT;'])
        sql('\n'.join(statements))
        print('Alice: 3 older orders and '+str(len(snapshots))+' daily profit records added in local '+settings['DB_NAME']+'. Existing orders preserved; cash and positions reconciled atomically.')
    return 0

if __name__=='__main__':
    try:raise SystemExit(main())
    except Exception as exc:
        print('History population stopped: '+str(exc) if isinstance(exc,ValueError) else 'History population stopped; check backend/database access.')
        raise SystemExit(1)
