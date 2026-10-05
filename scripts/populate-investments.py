#!/usr/bin/env python3
"""Populate the running local NSPay app through its atomic, authenticated investment API."""
import argparse
import json
from urllib import request, error
import uuid


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base-url', default='http://127.0.0.1:8082')
    args = parser.parse_args()

    def api(path, body=None, token=None):
        headers = {'Content-Type': 'application/json'}
        if token:
            headers['Authorization'] = 'Bearer ' + token
        req = request.Request(args.base_url.rstrip('/') + path,
                              data=None if body is None else json.dumps(body).encode(), headers=headers)
        with request.urlopen(req, timeout=35) as response:
            return json.load(response)

    # Reserved seeded application users only; no real user's existing portfolio is rewritten.
    plans = {'alice': [('AAPL', 2), ('MSFT', 1), ('SPY', 1)],
             'bob': [('NVDA', 2), ('QQQ', 1)]}
    try:
        for username, plan in plans.items():
            login = api('/api/login/', {'name': username, 'password': 'DemoPay123!'})
            token = login['token']
            portfolio = api('/api/investments', token=token)
            if portfolio['orderCount'] or portfolio['holdings']:
                print(username + ': existing portfolio preserved')
                continue
            account = api('/api/finance/overview', token=token)['account']
            if account['status'] != 'ACTIVE':
                print(username + ': restricted account preserved; no funds moved')
                continue
            prices = api('/api/investments/quotes', token=token)
            required = round(sum(float(prices[s]['eurPrice']) * quantity for s, quantity in plan) + 150, 2)
            deposit = round(max(0, required - float(portfolio['cash'])), 2)
            if deposit > float(account['amount']):
                print(username + ': insufficient account funds; no funds moved')
                continue
            if deposit:
                api('/api/investments/deposit', {'amount': deposit, 'requestKey': 'sample-portfolio-funding-v1'}, token)
            for symbol, quantity in plan:
                quote = api('/api/investments/' + symbol + '/quote', token=token)
                api('/api/investments/orders', {'symbol': symbol, 'side': 'BUY', 'quantity': quantity,
                    'quoteId': quote['id'], 'requestKey': str(uuid.uuid4())}, token)
            print(username + ': portfolio populated; main-account funds moved through the investment API')
        return 0
    except (error.HTTPError, error.URLError, KeyError, ValueError) as exc:
        # Never print login response/token or server request contents.
        print('Population stopped: ' + (str(exc.code) if isinstance(exc, error.HTTPError) else type(exc).__name__) +
              '. Check backend access and seeded-user credentials. Existing completed orders are preserved.')
        return 1


if __name__ == '__main__':
    raise SystemExit(main())
