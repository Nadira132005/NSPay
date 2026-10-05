package com.nspay.finance;

import com.fasterxml.jackson.databind.*;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** Two shared upstream connections cover the catalog. No per-user provider connection or SQL write per tick. */
@Service
public class CuckooStreamFeed {
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(CuckooStreamFeed.class);
    @Data public static class Tick {
        private final String symbol;private final BigDecimal price;private final Instant priceAt;private final BigDecimal change;
    }
    private final ObjectMapper mapper;private final boolean enabled;private final String url,seed;private final int generation;
    private final Map<String,Tick> latest=new ConcurrentHashMap<>();
    private final Map<String,Deque<Tick>> history=new ConcurrentHashMap<>();
    private final Map<String,BigDecimal> anchors=new ConcurrentHashMap<>();
    private final Map<Integer,InputStream> bodies=new ConcurrentHashMap<>();
    private final Map<Integer,Instant> activity=new ConcurrentHashMap<>();
    private final ExecutorService readers=Executors.newFixedThreadPool(2,r -> {Thread t=new Thread(r,"cuckoo-stream");t.setDaemon(true);return t;});
    private final ScheduledExecutorService watchdog=Executors.newSingleThreadScheduledExecutor(r -> {Thread t=new Thread(r,"cuckoo-watchdog");t.setDaemon(true);return t;});
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private volatile boolean stopping;
    public CuckooStreamFeed(ObjectMapper mapper,@Value("${market.provider:cuckootrade}") String provider,
        @Value("${market.streaming.enabled:true}") boolean enabled,@Value("${market.cuckootrade.clock:demo}") String clock,
        @Value("${market.cuckootrade.stream-url:https://cuckootrade.com/api/v1/stream}") String url,
        @Value("${market.cuckootrade.seed:42}") String seed,@Value("${market.cuckootrade.generation:1}") int generation){
        if(!Arrays.asList("demo","real").contains(clock))throw new IllegalArgumentException("CUCKOOTRADE_CLOCK must be demo or real");
        this.mapper=mapper;this.enabled=enabled&&"cuckootrade".equals(provider)&&"demo".equals(clock);this.url=url;this.seed=seed;this.generation=generation;
    }
    public boolean enabled(){return enabled;}
    @PostConstruct public void start(){
        if(!enabled)return;List<String> symbols=new ArrayList<>();for(MarketDataService.Instrument i:MarketDataService.instruments())symbols.add(i.getSymbol());
        for(int offset=0;offset<symbols.size();offset+=10){final int batch=offset/10;final String group=String.join(",",symbols.subList(offset,Math.min(offset+10,symbols.size())));readers.submit(() -> consume(batch,group));}
        watchdog.scheduleWithFixedDelay(() -> {for(Integer batch:bodies.keySet())if(activity.getOrDefault(batch,Instant.EPOCH).isBefore(Instant.now().minusSeconds(20)))close(batch);},5,5,TimeUnit.SECONDS);
    }
    private void consume(int batch,String symbols){
        int delay=1;
        while(!stopping){
            try{
                String query="symbols="+symbols+"&clock=demo&seed="+URLEncoder.encode(seed,StandardCharsets.UTF_8)+"&generation="+generation;
                HttpResponse<InputStream> response=client.send(HttpRequest.newBuilder(URI.create(url+"?"+query)).timeout(Duration.ofSeconds(10)).header("Accept","text/event-stream").GET().build(),HttpResponse.BodyHandlers.ofInputStream());
                if(response.statusCode()!=200){response.body().close();throw new IOException("Stream HTTP "+response.statusCode());}
                bodies.put(batch,response.body());activity.put(batch,Instant.now());
                try(BufferedReader reader=new BufferedReader(new InputStreamReader(response.body(),StandardCharsets.UTF_8))){
                    String line,event="";StringBuilder data=new StringBuilder();boolean connected=false;
                    while(!stopping&&(line=reader.readLine())!=null){
                        if(line.isEmpty()){
                            if("tick".equals(event)&&data.length()>0&&accept(mapper.readTree(data.toString()))){
                                activity.put(batch,Instant.now());delay=1;
                                if(!connected){log.info("Market stream connected for {}",symbols);connected=true;}
                            }
                            event="";data.setLength(0);
                        }
                        else if(line.startsWith("event:"))event=line.substring(6).trim();
                        else if(line.startsWith("data:")){if(data.length()>0)data.append('\n');data.append(line.substring(5).trim());if(data.length()>8192)throw new IOException("Oversized event");}
                    }
                }
            }catch(InterruptedException e){Thread.currentThread().interrupt();return;}catch(Exception e){if(!stopping)log.warn("Market stream disconnected for {}; reconnecting in {} seconds ({}: {})",symbols,delay,e.getClass().getSimpleName(),e.getMessage());}
            finally{close(batch);}
            if(!stopping)try{TimeUnit.SECONDS.sleep(delay);delay=Math.min(30,delay*2);}catch(InterruptedException e){Thread.currentThread().interrupt();return;}
        }
    }
    boolean accept(JsonNode data){
        try{
            String symbol=data.path("S").asText();if(MarketDataService.instruments().stream().noneMatch(i -> i.getSymbol().equals(symbol)))return false;
            BigDecimal price=new BigDecimal(data.path("p").asText());Instant at=Instant.parse(data.path("t").asText());
            if(price.signum()<=0||price.compareTo(new BigDecimal("1000000000"))>0||at.isAfter(Instant.now().plusSeconds(5))||at.isBefore(Instant.now().minusSeconds(15)))return false;
            Tick previous=latest.get(symbol);if(previous!=null&&!at.isAfter(previous.getPriceAt()))return false;
            BigDecimal anchor=anchors.computeIfAbsent(symbol,s -> price);Tick tick=new Tick(symbol,price,at,price.subtract(anchor).multiply(BigDecimal.valueOf(100)).divide(anchor,4,RoundingMode.HALF_UP));
            latest.put(symbol,tick);Deque<Tick> points=history.computeIfAbsent(symbol,s -> new ArrayDeque<>());
            synchronized(points){points.addLast(tick);while(points.size()>600)points.removeFirst();}
            return true;
        }catch(Exception ignored){/* Reject malformed frames without dropping valid prices. */}
        return false;
    }
    public Tick current(String symbol){Tick tick=latest.get(symbol);return tick!=null&&tick.getPriceAt().isAfter(Instant.now().minusSeconds(15))?tick:null;}
    public Map<String,Tick> current(){Map<String,Tick> result=new LinkedHashMap<>();for(MarketDataService.Instrument i:MarketDataService.instruments()){Tick tick=current(i.getSymbol());if(tick!=null)result.put(i.getSymbol(),tick);}return result;}
    public List<Tick> history(String symbol){Deque<Tick> points=history.get(symbol);if(points==null)return Collections.emptyList();synchronized(points){return new ArrayList<>(points);}}
    private void close(int batch){InputStream body=bodies.remove(batch);if(body!=null)try{body.close();}catch(IOException ignored){}}
    @PreDestroy public void stop(){stopping=true;for(Integer batch:new ArrayList<>(bodies.keySet()))close(batch);readers.shutdownNow();watchdog.shutdownNow();}
}
