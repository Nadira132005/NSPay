package com.nspay.finance;
import com.nspay.entities.*;
import com.nspay.repository.*;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import javax.validation.constraints.*;
import java.math.*;
import java.time.Instant;
import java.util.*;
@Service
public class InvestmentService {
    @io.swagger.v3.oas.annotations.media.Schema(name="InvestmentFunding")
    @Data public static class Funding {
        @NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) private BigDecimal amount;
        @NotBlank @Size(max=80) @io.swagger.v3.oas.annotations.media.Schema(description="Unique key for this command; preserve it when retrying.",example="9ca3a625-6c20-4a61-b8d0-920153a24105") private String requestKey;
    }
    @io.swagger.v3.oas.annotations.media.Schema(name="InvestmentOrder")
    @Data public static class Order {
        @NotBlank @io.swagger.v3.oas.annotations.media.Schema(example="AAPL") private String symbol;@NotBlank @io.swagger.v3.oas.annotations.media.Schema(description="ID returned by the server quote endpoint; valid for 60 seconds.") private String quoteId;
        @NotNull @Pattern(regexp="BUY|SELL") private String side;
        @NotNull @DecimalMin("0.000001") @Digits(integer=6,fraction=6) private BigDecimal quantity;
        @NotBlank @Size(max=80) @io.swagger.v3.oas.annotations.media.Schema(description="Unique key for this command; preserve it when retrying.",example="9ca3a625-6c20-4a61-b8d0-920153a24105") private String requestKey;
    }
    private final FinanceService finance;private final InvestmentWalletRepository wallets;private final InvestmentCommandRepository commands;
    private final PortfolioPerformanceService performance;private final InvestmentPositionRepository positions;private final org.springframework.transaction.support.TransactionTemplate snapshotTransaction;
    private final TradeOrderRepository trades;private final AccountRepository accounts;private final TransactionRepository transactions;private final MarketDataService market;
    public InvestmentService(FinanceService f,InvestmentWalletRepository w,InvestmentCommandRepository c,TradeOrderRepository t,AccountRepository a,TransactionRepository tx,MarketDataService m,InvestmentPositionRepository p,org.springframework.transaction.PlatformTransactionManager manager,PortfolioPerformanceService performance){this.performance=performance;finance=f;wallets=w;commands=c;trades=t;accounts=a;transactions=tx;market=m;positions=p;snapshotTransaction=new org.springframework.transaction.support.TransactionTemplate(manager);}
    private InvestmentWallet wallet(long id){return wallets.findByUserId(id).orElseGet(() -> {InvestmentWallet w=new InvestmentWallet();w.setUserId(id);return w;});}
    private List<InvestmentPosition> initialized(long id,InvestmentWallet w){
        List<InvestmentPosition> result=positions.findByUserIdOrderBySymbolAsc(id);
        if(!w.isPositionsInitialized()){
            // One-time, bounded-memory reconstruction for existing order histories, under the account lock.
            if(!result.isEmpty())throw new IllegalStateException("Uninitialized investment projection already contains positions");
            long after=0;Instant afterTime=Instant.EPOCH;List<TradeOrder> page;
            do{page=trades.findFollowing(id,afterTime,after,org.springframework.data.domain.PageRequest.of(0,200));
                for(TradeOrder order:page){after=order.getId();afterTime=order.getCreatedAt();if("FILLED".equals(order.getStatus()))apply(result,order);}
            }while(page.size()==200);
            positions.saveAll(result);w.setPositionsInitialized(true);wallets.save(w);
        }
        return result;
    }
    private void apply(List<InvestmentPosition> list,TradeOrder order){
        InvestmentPosition p=list.stream().filter(x -> x.getSymbol().equals(order.getSymbol())&&x.getProvider().equals(order.getProvider())).findFirst().orElseGet(() -> {
            InvestmentPosition created=new InvestmentPosition();created.setUserId(order.getUserId());created.setSymbol(order.getSymbol());created.setProvider(order.getProvider());list.add(created);return created;});
        if("BUY".equals(order.getSide())){p.setQuantity(p.getQuantity().add(order.getQuantity()));p.setCostBasis(p.getCostBasis().add(order.getTotal()));}
        else if("SELL".equals(order.getSide())){
            if(p.getQuantity().compareTo(order.getQuantity())<0)throw fail("Not enough units to sell from this price source");
            BigDecimal removed=p.getQuantity().compareTo(order.getQuantity())==0?p.getCostBasis():p.getCostBasis().multiply(order.getQuantity()).divide(p.getQuantity(),8,RoundingMode.HALF_UP);
            p.setQuantity(p.getQuantity().subtract(order.getQuantity()));p.setCostBasis(p.getCostBasis().subtract(removed));p.setRealizedGain(p.getRealizedGain().add(order.getTotal().subtract(removed)));
        }else throw new IllegalStateException("Unknown historical order side");
    }
    public org.springframework.data.domain.Page<TradeOrder> history(long id,int page,int size){
        finance.account(id,false);if(page<0||size<1||size>100)throw fail("Page must be nonnegative and size between 1 and 100");
        return trades.findByUserId(id,org.springframework.data.domain.PageRequest.of(page,size,org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,"createdAt","id")));
    }
    public Map<String,Object> portfolio(long id){
        // Finish the DB snapshot before doing external I/O: provider latency never holds an account lock.
        Map<String,Object> result=snapshotTransaction.execute(status -> {
            AccountModel account=finance.account(id,false);if(accounts.lockById(account.getId())==null)throw fail("Account unavailable");
            InvestmentWallet w=wallet(id);List<InvestmentPosition> held=initialized(id,w);
            Map<String,Object> view=new LinkedHashMap<>();view.put("cash",w.getBalance());view.put("walletVersion",w.getVersion());view.put("positions",held);return view;
        });
        @SuppressWarnings("unchecked") List<InvestmentPosition> held=(List<InvestmentPosition>)result.remove("positions");
        Set<String> symbols=new LinkedHashSet<>();for(InvestmentPosition p:held)if(p.getQuantity().signum()>0&&p.getProvider().equals(market.source())&&market.catalog().stream().anyMatch(i -> i.getSymbol().equals(p.getSymbol())))symbols.add(p.getSymbol());
        Map<String,MarketDataService.Quote> prices=Collections.emptyMap();String valuationError=null;
        try{prices=market.quotes(symbols);}catch(ResponseStatusException e){valuationError=e.getReason();}
        List<Map<String,Object>> holdings=new ArrayList<>();BigDecimal value=BigDecimal.ZERO,basis=BigDecimal.ZERO,realized=BigDecimal.ZERO;boolean complete=true;Instant priceAt=null;
        for(InvestmentPosition p:held){
            realized=realized.add(p.getRealizedGain());if(p.getQuantity().signum()==0)continue;basis=basis.add(p.getCostBasis());
            Map<String,Object> h=new LinkedHashMap<>();h.put("symbol",p.getSymbol());h.put("provider",p.getProvider());h.put("quantity",p.getQuantity());h.put("costBasis",FinanceService.cents(p.getCostBasis()));h.put("accountingBasis",p.getCostBasis());h.put("averageCost",p.getCostBasis().divide(p.getQuantity(),6,RoundingMode.HALF_UP));
            MarketDataService.Quote q=p.getProvider().equals(market.source())?prices.get(p.getSymbol()):null;
            if(q==null){complete=false;h.put("valuationStatus","UNAVAILABLE");h.put("marketValue",null);h.put("unrealizedGain",null);h.put("unrealizedGainPercent",null);h.put("price",null);h.put("eurPrice",null);h.put("priceAt",null);}
            else{BigDecimal marked=q.getEurPrice().multiply(p.getQuantity()),gain=marked.subtract(p.getCostBasis());value=value.add(marked);
                h.put("valuationStatus","CURRENT");h.put("price",q.getPrice());h.put("eurPrice",q.getEurPrice());h.put("marketValue",FinanceService.cents(marked));h.put("unrealizedGain",FinanceService.cents(gain));h.put("unrealizedGainPercent",percent(gain,p.getCostBasis()));h.put("priceAt",q.getPriceAt());
                if(priceAt==null||q.getPriceAt().isBefore(priceAt))priceAt=q.getPriceAt();}
            holdings.add(h);
        }
        BigDecimal gain=value.subtract(basis);result.put("holdings",holdings);result.put("holdingsValue",complete?FinanceService.cents(value):null);
        result.put("totalValue",complete?FinanceService.cents(value.add((BigDecimal)result.get("cash"))):null);result.put("costBasis",FinanceService.cents(basis));
        result.put("unrealizedGain",complete?FinanceService.cents(gain):null);result.put("unrealizedGainPercent",complete?percent(gain,basis):null);result.put("realizedGain",FinanceService.cents(realized));result.put("totalGain",complete?FinanceService.cents(realized.add(gain)):null);
        result.put("valuationStatus",complete?"COMPLETE":"INCOMPLETE");result.put("valuationError",valuationError);result.put("priceAt",priceAt);result.put("valuedAt",Instant.now());result.put("currency","EUR");
        for(Map<String,Object> h:holdings)h.put("allocationPercent",complete&&value.signum()>0?percent((BigDecimal)h.get("marketValue"),value):null);
        org.springframework.data.domain.Page<TradeOrder> history=history(id,0,20);result.put("orders",history.getContent());result.put("orderCount",history.getTotalElements());result.put("catalog",market.catalog());result.put("configured",market.configured());result.put("provider",market.provider());result.put("streaming",market.streaming());performance.record(id,((Number)result.remove("walletVersion")).longValue(),result);return result;
    }
    private static BigDecimal percent(BigDecimal gain,BigDecimal basis){return basis.signum()==0?BigDecimal.ZERO:gain.multiply(BigDecimal.valueOf(100)).divide(basis,2,RoundingMode.HALF_UP);}
    private Optional<InvestmentCommand> replay(long id,String key,String fingerprint){
        if(key==null || key.isBlank() || key.length()>80)throw fail("A request key is required");
        Optional<InvestmentCommand> old=commands.findByUserIdAndRequestKey(id,key);
        if(old.isPresent() && !old.get().getFingerprint().equals(fingerprint))throw new ResponseStatusException(HttpStatus.CONFLICT,"Request key was used for a different operation");return old;
    }
    private void remember(long id,String key,String fingerprint,Long orderId){InvestmentCommand c=new InvestmentCommand();c.setUserId(id);c.setRequestKey(key);c.setFingerprint(fingerprint);c.setOrderId(orderId);commands.save(c);}
    @Transactional
    public InvestmentWallet fund(long id,Funding request,boolean withdraw){
        AccountModel account=finance.account(id,true);InvestmentWallet wallet=wallet(id);
        BigDecimal amount=request.getAmount();if(amount==null || amount.signum()<=0 || amount.scale()>2 || amount.compareTo(new BigDecimal("10000000000"))>=0)throw fail("Enter a positive amount with at most two decimal places");
        String fingerprint=(withdraw?"WITHDRAW:":"DEPOSIT:")+amount.stripTrailingZeros().toPlainString();
        if(replay(id,request.getRequestKey(),fingerprint).isPresent())return wallet;
        BigDecimal main=FinanceService.cents(BigDecimal.valueOf(account.getAmount()));
        if((withdraw?wallet.getBalance():main).compareTo(amount)<0)throw fail("Insufficient available cash");
        account.setAmount((withdraw?main.add(amount):main.subtract(amount)).doubleValue());wallet.setBalance(withdraw?wallet.getBalance().subtract(amount):wallet.getBalance().add(amount));
        accounts.save(account);wallets.save(wallet);ledger(account,amount,withdraw?"INVESTMENT_WITHDRAWAL":"INVESTMENT_DEPOSIT","Investment cash");remember(id,request.getRequestKey(),fingerprint,null);return wallet;
    }
    @Transactional
    public TradeOrder order(long id,Order request){
        AccountModel account=finance.account(id,true);
        if(request.getQuantity()==null || request.getQuantity().signum()<=0 || request.getQuantity().scale()>6 || request.getQuantity().compareTo(new BigDecimal("1000000"))>=0 || !Arrays.asList("BUY","SELL").contains(request.getSide()))throw fail("Invalid order quantity or side");
        String fingerprint=request.getSide()+":"+request.getSymbol()+":"+request.getQuantity().stripTrailingZeros().toPlainString()+":"+request.getQuoteId();
        Optional<InvestmentCommand> previous=replay(id,request.getRequestKey(),fingerprint);if(previous.isPresent())return trades.lockById(previous.get().getOrderId()).get();
        MarketDataService.Quote q=market.accepted(request.getSymbol(),request.getQuoteId());
        BigDecimal total=FinanceService.cents(q.getEurPrice().multiply(request.getQuantity()));if(total.signum()<=0)throw fail("Order value must be at least EUR 0.01");
        InvestmentWallet w=wallet(id);List<InvestmentPosition> held=initialized(id,w);
        if("BUY".equals(request.getSide())){if(w.getBalance().compareTo(total)<0)throw fail("Add sufficient investment cash before buying");w.setBalance(w.getBalance().subtract(total));}
        else {w.setBalance(w.getBalance().add(total));}
        wallets.save(w);TradeOrder order=new TradeOrder();order.setUserId(id);order.setSymbol(request.getSymbol());order.setSide(request.getSide());order.setQuantity(request.getQuantity());order.setPrice(q.getEurPrice());order.setTotal(total);order.setStatus("FILLED");order.setMarketPrice(q.getPrice());order.setExchangeRate(q.getExchangeRate());order.setPriceAt(q.getPriceAt());order.setExchangeRateAt(q.getExchangeRateAt());order.setQuoteId(q.getId());order.setProvider(q.getMarketProvider());apply(held,order);positions.saveAll(held);trades.save(order);
        remember(id,request.getRequestKey(),fingerprint,order.getId());return order;
    }
    private void ledger(AccountModel account,BigDecimal amount,String type,String reference){TransactionModel t=new TransactionModel();t.setCardNumber(account.getCardNumber());t.setAmount(amount.doubleValue());t.setReturnCode("00");t.setTimestamp(java.sql.Timestamp.from(Instant.now()));t.setTransactionType(type);t.setCounterpartyIban(reference);transactions.save(t);}
    private static ResponseStatusException fail(String m){return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}
}
