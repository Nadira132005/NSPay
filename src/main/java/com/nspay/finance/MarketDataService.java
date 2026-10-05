package com.nspay.finance;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.server.ResponseStatusException;
import java.net.*;
import java.net.http.*;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MarketDataService {
    @Data public static class Instrument {
        private final String symbol;private final String name;private final String group;private final String currency="USD";
    }
    @Data public static class Quote {
        private String id;private String symbol;private BigDecimal price;private BigDecimal eurPrice;private BigDecimal exchangeRate;private BigDecimal change;
        private Instant priceAt;private Instant fetchedAt;private Instant expiresAt;private Instant exchangeRateAt;private boolean marketOpen;
        private String clock="real";private String provider="Twelve Data";private String marketProvider="twelve-data";private boolean synthetic;private String exchangeRateSource="Twelve Data";
    }
    private static final List<Instrument> CATALOG=Collections.unmodifiableList(Arrays.asList(
        new Instrument("AAPL","Apple","Technology"),new Instrument("MSFT","Microsoft","Technology"),new Instrument("NVDA","NVIDIA","Technology"),
        new Instrument("GOOGL","Alphabet","Technology"),new Instrument("AMZN","Amazon","Consumer"),new Instrument("TSLA","Tesla","Consumer"),
        new Instrument("JPM","JPMorgan Chase","Finance"),new Instrument("V","Visa","Finance"),new Instrument("SPY","SPDR S&P 500 ETF","ETFs"),
        new Instrument("VOO","Vanguard S&P 500 ETF","ETFs"),new Instrument("QQQ","Invesco QQQ ETF","ETFs")));
    @org.springframework.beans.factory.annotation.Autowired(required=false) private CuckooStreamFeed stream;
    private final Map<String,Quote> liveQuotes=new ConcurrentHashMap<>();
    public boolean streaming(){return stream!=null&&stream.enabled();}
    public static List<Instrument> instruments(){return CATALOG;}
    private final String provider,apiKey,baseUrl,seed;
    private final int generation;
    private final BigDecimal sampleRate;
    private final ObjectMapper mapper;
    private final MarketQuoteRepository snapshots;
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final Object quoteLock=new Object();
    private final Map<String,CachedChart> charts=new ConcurrentHashMap<>();
    private final Map<String,Object> chartLocks=new ConcurrentHashMap<>();
    private int requests;private Instant window=Instant.now();
    private static class CachedChart {final JsonNode data;final Instant expires;CachedChart(JsonNode d){data=d;expires=Instant.now().plusSeconds(60);}}
    public MarketDataService(@Value("${market.provider:cuckootrade}") String provider,
        @Value("${market.twelve-data.api-key:}") String key,@Value("${market.cuckootrade.base-url:https://cuckootrade.com/api/v1/alpaca}") String baseUrl,
        @Value("${market.cuckootrade.seed:42}") String seed,@Value("${market.cuckootrade.generation:1}") int generation,
        @Value("${market.cuckootrade.usd-eur-rate:0.92}") BigDecimal rate,ObjectMapper mapper,MarketQuoteRepository snapshots){
        if(!Arrays.asList("cuckootrade","twelve-data").contains(provider))throw new IllegalArgumentException("Unsupported MARKET_PROVIDER");
        if(rate.signum()<=0 || rate.compareTo(new BigDecimal("100"))>0)throw new IllegalArgumentException("USD/EUR sample rate must be positive and at most 100");
        this.provider=provider;apiKey=key;this.baseUrl=baseUrl.replaceAll("/+$","");this.seed=seed;this.generation=generation;sampleRate=rate;this.mapper=mapper;this.snapshots=snapshots;
    }
    public List<Instrument> catalog(){return CATALOG;}
    public String provider(){return provider;}
    public BigDecimal liveExchangeRate(){return sampleRate;}
    // Dataset and conversion are part of the identity: switching seed must never reprice existing holdings.
    public String source(){return "cuckootrade".equals(provider)?"cuckoo-"+datasetHash():"twelve-data";}
    private String datasetHash(){try{byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest((seed+":"+generation+":"+sampleRate.stripTrailingZeros().toPlainString()).getBytes(java.nio.charset.StandardCharsets.UTF_8));return java.util.HexFormat.of().formatHex(digest).substring(0,16);}catch(Exception e){throw new IllegalStateException(e);}}
    public boolean configured(){return "cuckootrade".equals(provider)||!apiKey.isBlank();}
    private void symbol(String symbol){if(CATALOG.stream().noneMatch(i -> i.getSymbol().equals(symbol)))throw fail(HttpStatus.BAD_REQUEST,"Choose a listed instrument");}
    private synchronized void reserve(){
        if(window.plusSeconds(60).isBefore(Instant.now())){requests=0;window=Instant.now();}
        if(requests>=("cuckootrade".equals(provider)?55:8))throw fail(HttpStatus.TOO_MANY_REQUESTS,"Market data request limit reached. Try again in one minute.");requests++;
    }
    protected JsonNode fetch(String endpoint,String parameters){
        if(!configured())throw fail(HttpStatus.SERVICE_UNAVAILABLE,"Market data is not configured. Set TWELVE_DATA_API_KEY on the backend.");reserve();
        String url="cuckootrade".equals(provider)?baseUrl+"/"+endpoint+"?"+parameters+"&seed="+encode(seed)+"&generation="+generation:
            "https://api.twelvedata.com/"+endpoint+"?"+parameters+"&apikey="+encode(apiKey);
        try{
            HttpResponse<String> response=client.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build(),HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()==429)throw fail(HttpStatus.TOO_MANY_REQUESTS,"Market data provider quota reached. Try again in one minute.");
            JsonNode data=mapper.readTree(response.body());
            if(response.statusCode()!=200 || data==null || "error".equals(data.path("status").asText()))throw fail(HttpStatus.SERVICE_UNAVAILABLE,"Market data provider is temporarily unavailable.");return data;
        }catch(ResponseStatusException e){throw e;}catch(InterruptedException e){Thread.currentThread().interrupt();throw fail(HttpStatus.SERVICE_UNAVAILABLE,"Market data request interrupted");}
        catch(Exception e){throw fail(HttpStatus.SERVICE_UNAVAILABLE,"Market data is temporarily unavailable");}
    }
    private static String encode(String value){return URLEncoder.encode(value,java.nio.charset.StandardCharsets.UTF_8);}
    private Map<String,Quote> cached(){
        Map<String,Quote> result=new HashMap<>();
        for(MarketQuote row:snapshots.findByProviderAndExpiresAtAfter(source(),Instant.now()))try{
            Quote q=mapper.readValue(row.getPayload(),Quote.class);Quote old=result.get(q.getSymbol());
            if(old==null||old.getFetchedAt().isBefore(q.getFetchedAt()))result.put(q.getSymbol(),q);
        }catch(Exception e){throw fail(HttpStatus.SERVICE_UNAVAILABLE,"Stored market price cannot be read");}
        return result;
    }
    public Quote quote(String symbol){symbol(symbol);return quotes(Collections.singleton(symbol)).get(symbol);}
    public Map<String,Quote> quotes(Collection<String> symbols){
        symbols.forEach(this::symbol);if(symbols.isEmpty())return Collections.emptyMap();
        if(streaming()){
            Map<String,Quote> result=new LinkedHashMap<>();synchronized(quoteLock){
                for(String symbol:symbols){CuckooStreamFeed.Tick tick=stream.current(symbol);
                    if(tick==null)throw fail(HttpStatus.SERVICE_UNAVAILABLE,"Price stream is reconnecting. Please try again shortly.");
                    Quote q=liveQuotes.get(symbol);
                    if(q==null||!q.getPriceAt().equals(tick.getPriceAt())||!q.getExpiresAt().isAfter(Instant.now())){
                        q=new Quote();q.setClock("demo");q.setSymbol(symbol);q.setPrice(tick.getPrice());q.setPriceAt(tick.getPriceAt());q.setChange(tick.getChange());
                        q.setProvider("CuckooTrade");q.setMarketProvider(source());q.setSynthetic(true);q.setExchangeRate(sampleRate);q.setExchangeRateAt(Instant.now());q.setExchangeRateSource("Configured sample rate");q.setMarketOpen(true);
                        finish(q);snapshots.save(row(q));liveQuotes.put(symbol,q);
                    }result.put(symbol,q);
                }
            }return result;
        }
        Map<String,Quote> result=cached();
        if(!result.keySet().containsAll(symbols))synchronized(quoteLock){
            result=cached();if(!result.keySet().containsAll(symbols)){
                if("cuckootrade".equals(provider))result.putAll(cuckooQuotes());
                else for(String s:symbols)if(!result.containsKey(s))result.put(s,twelveQuote(s));
            }
        }
        Map<String,Quote> selected=new LinkedHashMap<>();for(String s:symbols)selected.put(s,result.get(s));return selected;
    }
    private Map<String,Quote> cuckooQuotes(){
        String symbols=String.join(",",CATALOG.stream().map(Instrument::getSymbol).collect(java.util.stream.Collectors.toList()));
        JsonNode latest=fetch("v2/stocks/bars/latest","symbols="+symbols).path("bars");
        JsonNode daily=fetch("v2/stocks/bars","symbols="+symbols+"&timeframe=1Day&start="+LocalDate.now(ZoneOffset.UTC).minusDays(10)+"&sort=desc&limit=500").path("bars");
        Map<String,Quote> result=new LinkedHashMap<>();List<MarketQuote> rows=new ArrayList<>();
        try{
            for(Instrument instrument:CATALOG){String s=instrument.getSymbol();JsonNode bar=latest.path(s);Quote q=new Quote();
                q.setSymbol(s);q.setPrice(new BigDecimal(bar.path("c").asText()));q.setPriceAt(Instant.parse(bar.path("t").asText()));
                q.setExchangeRate(sampleRate);q.setExchangeRateAt(Instant.now());q.setExchangeRateSource("Configured sample rate");q.setSynthetic(true);q.setProvider("CuckooTrade");q.setMarketProvider(source());
                BigDecimal previous=null;
                // Daily bars are timestamped at NYSE midnight. Skip the current price's session.
                LocalDate session=q.getPriceAt().atZone(ZoneId.of("America/New_York")).toLocalDate();
                for(JsonNode b:daily.path(s))if(Instant.parse(b.path("t").asText()).atZone(ZoneId.of("America/New_York")).toLocalDate().isBefore(session)){previous=new BigDecimal(b.path("c").asText());break;}
                q.setChange(previous!=null&&previous.signum()>0?q.getPrice().subtract(previous).multiply(BigDecimal.valueOf(100)).divide(previous,4,RoundingMode.HALF_UP):BigDecimal.ZERO);
                q.setMarketOpen(q.getPriceAt().isAfter(Instant.now().minusSeconds(180)));finish(q);rows.add(row(q));result.put(s,q);
            }
            snapshots.saveAll(rows);return result;
        }catch(ResponseStatusException e){throw e;}catch(Exception e){throw fail(HttpStatus.SERVICE_UNAVAILABLE,"Provider returned invalid stock prices. Orders are unavailable.");}
    }
    private Quote twelveQuote(String symbol){
        JsonNode data=fetch("quote","symbol="+symbol+"&timezone=UTC"),fx=fetch("exchange_rate","symbol=USD%2FEUR");
        try{
            if(!"USD".equals(data.path("currency").asText()))throw new IllegalArgumentException();
            Quote q=new Quote();q.setSymbol(symbol);q.setPrice(new BigDecimal(data.path("close").asText()));q.setExchangeRate(new BigDecimal(fx.path("rate").asText()));
            q.setExchangeRateAt(Instant.ofEpochSecond(fx.path("timestamp").asLong()));q.setChange(new BigDecimal(data.path("percent_change").asText("0")));
            q.setPriceAt(Instant.ofEpochSecond(data.path("timestamp").asLong()));q.setMarketOpen(data.path("is_market_open").asBoolean());finish(q);snapshots.save(row(q));return q;
        }catch(ResponseStatusException e){throw e;}catch(Exception e){throw fail(HttpStatus.SERVICE_UNAVAILABLE,"Provider returned invalid stock prices. Orders are unavailable.");}
    }
    private void finish(Quote q){
        Instant now=Instant.now();
        if(q.getPrice().signum()<=0||q.getExchangeRate().signum()<=0||q.getPriceAt().isAfter(now.plusSeconds(60))||q.getPriceAt().isBefore(now.minusSeconds(q.isMarketOpen()?300:7*86400))||q.getExchangeRateAt().isAfter(now.plusSeconds(60))||q.getExchangeRateAt().isBefore(now.minusSeconds(5*86400)))throw fail(HttpStatus.SERVICE_UNAVAILABLE,"Provider returned a stale or invalid price. Orders are unavailable.");
        q.setId(UUID.randomUUID().toString());q.setEurPrice(q.getPrice().multiply(q.getExchangeRate()).setScale(6,RoundingMode.HALF_UP));q.setFetchedAt(now);q.setExpiresAt(now.plusSeconds(60));
    }
    private MarketQuote row(Quote q){try{MarketQuote row=new MarketQuote();row.setId(q.getId());row.setProvider(source());row.setSymbol(q.getSymbol());row.setExpiresAt(q.getExpiresAt());row.setPayload(mapper.writeValueAsString(q));return row;}catch(Exception e){throw new IllegalStateException(e);}}
    public Quote accepted(String symbol,String id){
        symbol(symbol);MarketQuote row=snapshots.findById(id).orElseThrow(() -> fail(HttpStatus.CONFLICT,"Quote expired. Refresh the price and review the order again."));
        if(!row.getSymbol().equals(symbol)||!row.getProvider().equals(source())||!row.getExpiresAt().isAfter(Instant.now()))throw fail(HttpStatus.CONFLICT,"Quote expired. Refresh the price and review the order again.");
        try{Quote q=mapper.readValue(row.getPayload(),Quote.class);
            if(streaming()&&(!"demo".equals(q.getClock())||stream.current(symbol)==null))throw fail(HttpStatus.CONFLICT,"Live price unavailable or price mode changed. Refresh and review the order again.");
            if(!streaming()&&"demo".equals(q.getClock()))throw fail(HttpStatus.CONFLICT,"Price mode changed. Refresh and review the order again.");return q;
        }catch(ResponseStatusException e){throw e;}catch(Exception e){throw fail(HttpStatus.SERVICE_UNAVAILABLE,"Stored quote cannot be read");}
    }
    public JsonNode chart(String symbol,String range){
        symbol(symbol);
        if("LIVE".equals(range)){
            if(!streaming())throw fail(HttpStatus.BAD_REQUEST,"Live chart requires CuckooTrade streaming");
            ObjectNode data=mapper.createObjectNode();data.put("status","ok");data.put("provider","CuckooTrade");data.put("synthetic",true);data.put("clock","demo");
            ObjectNode meta=data.putObject("meta");meta.put("symbol",symbol);meta.put("currency","USD");meta.put("interval","1sec");meta.put("exchange_timezone","UTC");
            ArrayNode values=data.putArray("values");List<CuckooStreamFeed.Tick> ticks=stream.history(symbol);Collections.reverse(ticks);
            for(CuckooStreamFeed.Tick tick:ticks){ObjectNode bar=values.addObject();bar.put("datetime",tick.getPriceAt().toString());for(String field:Arrays.asList("open","high","low","close"))bar.put(field,tick.getPrice().toPlainString());bar.put("volume","0");}return data;
        }
        String interval;int count,days;
        switch(range){case "1D":interval="5Min";count=78;days=10;break;case "1W":interval="1Hour";count=35;days=10;break;case "1M":interval="1Day";count=22;days=45;break;case "1Y":interval="1Day";count=252;days=400;break;default:throw fail(HttpStatus.BAD_REQUEST,"Choose 1D, 1W, 1M or 1Y");}
        String key=symbol+range;CachedChart cached=charts.get(key);if(cached!=null&&cached.expires.isAfter(Instant.now()))return cached.data;
        // Independent symbols/ranges can load concurrently; duplicate requests for one chart coalesce.
        synchronized(chartLocks.computeIfAbsent(key,k -> new Object())){
            cached=charts.get(key);if(cached!=null&&cached.expires.isAfter(Instant.now()))return cached.data;JsonNode data;
            if("cuckootrade".equals(provider)){
                JsonNode bars=fetch("v2/stocks/bars","symbols="+symbol+"&timeframe="+interval+"&start="+LocalDate.now(ZoneOffset.UTC).minusDays(days)+"&sort=desc&limit="+count).path("bars").path(symbol);
                if(!bars.isArray()||bars.isEmpty())throw fail(HttpStatus.SERVICE_UNAVAILABLE,"No chart data available");
                ObjectNode normalized=mapper.createObjectNode();normalized.put("status","ok");normalized.put("provider","CuckooTrade");normalized.put("synthetic",true);
                ObjectNode meta=normalized.putObject("meta");meta.put("symbol",symbol);meta.put("currency","USD");meta.put("interval",interval);meta.put("exchange_timezone","UTC");
                ArrayNode values=normalized.putArray("values");
                try{for(JsonNode bar:bars){if(new BigDecimal(bar.path("c").asText()).signum()<=0)throw new IllegalArgumentException();
                    ObjectNode value=values.addObject();value.put("datetime",Instant.parse(bar.path("t").asText()).toString());
                    for(String[] field:new String[][]{{"open","o"},{"high","h"},{"low","l"},{"close","c"},{"volume","v"}})value.put(field[0],bar.path(field[1]).asText());}
                }catch(Exception e){throw fail(HttpStatus.SERVICE_UNAVAILABLE,"Provider returned invalid chart data");}data=normalized;
            }else{String td=interval.equals("5Min")?"5min":interval.equals("1Hour")?"1h":"1day";data=fetch("time_series","symbol="+symbol+"&interval="+td+"&outputsize="+count+"&timezone=UTC");
                if(!data.path("values").isArray()||data.path("values").isEmpty())throw fail(HttpStatus.SERVICE_UNAVAILABLE,"No chart data available");}
            charts.put(key,new CachedChart(data));return data;
        }
    }
    @Scheduled(fixedDelay=300000) public void cleanExpiredQuotes(){snapshots.deleteExpired(Instant.now().minusSeconds(300));}
    private static ResponseStatusException fail(HttpStatus s,String m){return new ResponseStatusException(s,m);}
}
