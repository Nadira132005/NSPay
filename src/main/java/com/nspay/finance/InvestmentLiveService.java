package com.nspay.finance;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class InvestmentLiveService {
    private final CuckooStreamFeed feed;private final MarketDataService market;private final InvestmentService investments;
    private final Map<SseEmitter,Client> clients=new ConcurrentHashMap<>();
    private final Map<Long,Baseline> baselines=new ConcurrentHashMap<>();
    private final ScheduledExecutorService clock=Executors.newSingleThreadScheduledExecutor(r -> {Thread t=new Thread(r,"investment-broadcast");t.setDaemon(true);return t;});
    private final ThreadPoolExecutor writers=new ThreadPoolExecutor(4,4,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(200),r -> {Thread t=new Thread(r,"investment-client");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
    private static class Client {final long user;final AtomicBoolean sending=new AtomicBoolean();Client(long user){this.user=user;}}
    private static class Baseline {final Map<String,Object> view;final Instant expires=Instant.now().plusSeconds(15);Baseline(Map<String,Object> view){this.view=view;}}
    public InvestmentLiveService(CuckooStreamFeed feed,MarketDataService market,InvestmentService investments){this.feed=feed;this.market=market;this.investments=investments;}
    @PostConstruct void start(){clock.scheduleWithFixedDelay(this::broadcast,1,1,TimeUnit.SECONDS);}
    public synchronized SseEmitter subscribe(long user){
        if(!feed.enabled())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Live stream is not enabled");
        if(clients.size()>=200)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Live stream capacity reached. Please retry shortly.");
        // Check ownership/account access before establishing a stream. Reuse one baseline per connected user.
        baseline(user);SseEmitter emitter=new SseEmitter(20*60*1000L);clients.put(emitter,new Client(user));
        Runnable remove=() -> {clients.remove(emitter);if(clients.values().stream().noneMatch(c -> c.user==user))baselines.remove(user);};
        emitter.onCompletion(remove);emitter.onTimeout(remove);emitter.onError(e -> remove.run());return emitter;
    }
    public void invalidate(long user){baselines.remove(user);}
    private Map<String,Object> baseline(long user){
        Baseline cached=baselines.get(user);if(cached==null||cached.expires.isBefore(Instant.now())){Map<String,Object> view=investments.portfolio(user);view.put("liveExchangeRate",market.liveExchangeRate());cached=new Baseline(view);baselines.put(user,cached);}return cached.view;
    }
    private void broadcast(){
        if(clients.isEmpty())return;
        Map<String,CuckooStreamFeed.Tick> prices=feed.current();Map<Long,Map<String,Object>> frames=new HashMap<>();
        for(Map.Entry<SseEmitter,Client> entry:clients.entrySet()){
            SseEmitter emitter=entry.getKey();Client client=entry.getValue();
            if(!client.sending.compareAndSet(false,true))continue; // Drop intermediate frames for slow clients instead of accumulating ticks.
            try{
                Map<String,Object> frame=frames.get(client.user);
                if(frame==null){frame=new LinkedHashMap<>();frame.put("prices",prices);frame.put("exchangeRate",market.liveExchangeRate());frame.put("marketProvider",market.source());frame.put("portfolio",mark(baseline(client.user),prices,market.source()));frame.put("at",Instant.now());frame.put("status",prices.size()==market.catalog().size()?"LIVE":"RECONNECTING");frames.put(client.user,frame);}
                final Map<String,Object> payload=frame;
                writers.execute(() -> {try{emitter.send(SseEmitter.event().name("prices").reconnectTime(3000).data(payload));}catch(Exception e){clients.remove(emitter);emitter.complete();}finally{client.sending.set(false);}});
            }catch(Exception e){client.sending.set(false);clients.remove(emitter);emitter.completeWithError(e);}
        }
        Set<Long> active=new HashSet<>();for(Client c:clients.values())active.add(c.user);baselines.keySet().retainAll(active);
    }
    /** Decimal marking from owned accounting state; does not mutate cash or the position projection. */
    @SuppressWarnings("unchecked") static Map<String,Object> mark(Map<String,Object> base,Map<String,CuckooStreamFeed.Tick> prices,String source){
        Map<String,Object> result=new LinkedHashMap<>(base);List<Map<String,Object>> holdings=new ArrayList<>();
        BigDecimal value=BigDecimal.ZERO,basis=BigDecimal.ZERO;boolean complete=true;Instant priceAt=null;
        for(Map<String,Object> original:(List<Map<String,Object>>)base.get("holdings")){
            Map<String,Object> holding=new LinkedHashMap<>(original);BigDecimal cost=(BigDecimal)holding.get("accountingBasis");if(cost==null)cost=(BigDecimal)holding.get("costBasis");basis=basis.add(cost);
            CuckooStreamFeed.Tick tick=source.equals(holding.get("provider"))?prices.get(holding.get("symbol")):null;
            if(tick==null){complete=false;for(String field:Arrays.asList("price","eurPrice","marketValue","unrealizedGain","unrealizedGainPercent","allocationPercent","priceAt"))holding.put(field,null);holding.put("valuationStatus","UNAVAILABLE");}
            else{
                BigDecimal fx=(BigDecimal)base.get("liveExchangeRate"),eur=tick.getPrice().multiply(fx).setScale(6,RoundingMode.HALF_UP);
                BigDecimal marked=eur.multiply((BigDecimal)holding.get("quantity")),gain=marked.subtract(cost);value=value.add(marked);
                if(priceAt==null||tick.getPriceAt().isBefore(priceAt))priceAt=tick.getPriceAt();holding.put("price",tick.getPrice());holding.put("eurPrice",eur);holding.put("marketValue",FinanceService.cents(marked));holding.put("unrealizedGain",FinanceService.cents(gain));holding.put("unrealizedGainPercent",percentage(gain,cost));holding.put("priceAt",tick.getPriceAt());holding.put("valuationStatus","CURRENT");
            }holdings.add(holding);
        }
        for(Map<String,Object> h:holdings)h.put("allocationPercent",complete?percentage((BigDecimal)h.get("marketValue"),value):null);
        BigDecimal gain=value.subtract(basis);result.put("holdings",holdings);result.put("holdingsValue",complete?FinanceService.cents(value):null);result.put("totalValue",complete?FinanceService.cents(value.add((BigDecimal)base.get("cash"))):null);
        result.put("unrealizedGain",complete?FinanceService.cents(gain):null);result.put("unrealizedGainPercent",complete?percentage(gain,basis):null);result.put("totalGain",complete?FinanceService.cents(gain.add((BigDecimal)base.get("realizedGain"))):null);result.put("valuationStatus",complete?"COMPLETE":"INCOMPLETE");result.put("valuationError",complete?null:"Live prices are reconnecting");result.put("priceAt",priceAt);result.put("valuedAt",Instant.now());
        result.remove("orders");result.remove("catalog");result.remove("liveExchangeRate");return result;
    }
    private static BigDecimal percentage(BigDecimal amount,BigDecimal basis){return basis.signum()==0?BigDecimal.ZERO:amount.multiply(BigDecimal.valueOf(100)).divide(basis,2,RoundingMode.HALF_UP);}
    @PreDestroy void stop(){clock.shutdownNow();for(SseEmitter emitter:clients.keySet())emitter.complete();writers.shutdownNow();}
}
