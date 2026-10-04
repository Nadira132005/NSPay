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
    private final TradeOrderRepository trades;private final AccountRepository accounts;private final TransactionRepository transactions;private final MarketDataService market;
    public InvestmentService(FinanceService f,InvestmentWalletRepository w,InvestmentCommandRepository c,TradeOrderRepository t,AccountRepository a,TransactionRepository tx,MarketDataService m){finance=f;wallets=w;commands=c;trades=t;accounts=a;transactions=tx;market=m;}
    private InvestmentWallet wallet(long id){return wallets.findByUserId(id).orElseGet(() -> {InvestmentWallet w=new InvestmentWallet();w.setUserId(id);return w;});}
    public Map<String,Object> portfolio(long id){finance.account(id,false);Map<String,Object> result=new LinkedHashMap<>();result.put("cash",wallet(id).getBalance());result.put("orders",trades.findByUserIdOrderByCreatedAtDesc(id));result.put("catalog",market.catalog());result.put("configured",market.configured());return result;}
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
        Optional<InvestmentCommand> previous=replay(id,request.getRequestKey(),fingerprint);if(previous.isPresent())return trades.findById(previous.get().getOrderId()).get();
        MarketDataService.Quote q=market.accepted(request.getSymbol(),request.getQuoteId());
        BigDecimal total=FinanceService.cents(q.getEurPrice().multiply(request.getQuantity()));if(total.signum()<=0)throw fail("Order value must be at least EUR 0.01");
        InvestmentWallet w=wallet(id);
        if("BUY".equals(request.getSide())){if(w.getBalance().compareTo(total)<0)throw fail("Add sufficient investment cash before buying");w.setBalance(w.getBalance().subtract(total));}
        else {BigDecimal units=trades.findByUserIdOrderByCreatedAtDesc(id).stream().filter(t -> t.getSymbol().equals(request.getSymbol())).map(t -> "BUY".equals(t.getSide())?t.getQuantity():t.getQuantity().negate()).reduce(BigDecimal.ZERO,BigDecimal::add);if(units.compareTo(request.getQuantity())<0)throw fail("Not enough units to sell");w.setBalance(w.getBalance().add(total));}
        wallets.save(w);TradeOrder order=new TradeOrder();order.setUserId(id);order.setSymbol(request.getSymbol());order.setSide(request.getSide());order.setQuantity(request.getQuantity());order.setPrice(q.getEurPrice());order.setTotal(total);order.setStatus("FILLED");order.setMarketPrice(q.getPrice());order.setExchangeRate(q.getExchangeRate());order.setPriceAt(q.getPriceAt());order.setExchangeRateAt(q.getExchangeRateAt());order.setQuoteId(q.getId());trades.save(order);
        remember(id,request.getRequestKey(),fingerprint,order.getId());return order;
    }
    private void ledger(AccountModel account,BigDecimal amount,String type,String reference){TransactionModel t=new TransactionModel();t.setCardNumber(account.getCardNumber());t.setAmount(amount.doubleValue());t.setReturnCode("00");t.setTimestamp(java.sql.Timestamp.from(Instant.now()));t.setTransactionType(type);t.setCounterpartyIban(reference);transactions.save(t);}
    private static ResponseStatusException fail(String m){return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}
}
