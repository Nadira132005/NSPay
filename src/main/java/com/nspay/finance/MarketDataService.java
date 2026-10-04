package com.nspay.finance;
import com.fasterxml.jackson.databind.*;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.net.*;
import java.net.http.*;
import java.math.*;
import java.time.*;
import java.util.*;
@Service
public class MarketDataService {
    @Data public static class Instrument {
        private final String symbol;private final String name;private final String group;private final String currency="USD";
    }
    @Data public static class Quote {
        private String id;private String symbol;private BigDecimal price;private BigDecimal eurPrice;private BigDecimal exchangeRate;private BigDecimal change;
        private Instant priceAt;private Instant fetchedAt;private Instant expiresAt;private Instant exchangeRateAt;private boolean marketOpen;private String provider="Twelve Data";
    }
    private static final List<Instrument> CATALOG=Arrays.asList(
        new Instrument("AAPL","Apple","Technology"),new Instrument("MSFT","Microsoft","Technology"),new Instrument("NVDA","NVIDIA","Technology"),
        new Instrument("GOOGL","Alphabet","Technology"),new Instrument("AMZN","Amazon","Consumer"),new Instrument("TSLA","Tesla","Consumer"),
        new Instrument("JPM","JPMorgan Chase","Finance"),new Instrument("V","Visa","Finance"),new Instrument("VOO","Vanguard S&P 500 ETF","ETFs"),new Instrument("QQQ","Invesco QQQ ETF","ETFs"));
    private final String apiKey;
    private final ObjectMapper mapper;
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final Map<String,Quote> quotes=new HashMap<>();
    private final Map<String,JsonNode> charts=new HashMap<>();
    private final Map<String,Instant> chartTimes=new HashMap<>();
    private int requests;private Instant window=Instant.now();
    private JsonNode fx;private Instant fxAt=Instant.EPOCH;
    public MarketDataService(@Value("${market.twelve-data.api-key:}") String key,ObjectMapper mapper){this.apiKey=key;this.mapper=mapper;}
    public List<Instrument> catalog(){return CATALOG;}
    public boolean configured(){return !apiKey.isBlank();}
    private void symbol(String symbol){if(CATALOG.stream().noneMatch(i -> i.getSymbol().equals(symbol)))throw fail(HttpStatus.BAD_REQUEST,"Choose a listed instrument");}
    private JsonNode fetch(String endpoint,String parameters){
        if(!configured())throw fail(HttpStatus.SERVICE_UNAVAILABLE,"Market data is not configured. Set TWELVE_DATA_API_KEY on the backend.");
        if(window.plusSeconds(60).isBefore(Instant.now())){requests=0;window=Instant.now();}
        if(requests>=8)throw fail(HttpStatus.TOO_MANY_REQUESTS,"Market data request limit reached. Try again in one minute.");
        requests++;
        try{
            URI uri=URI.create("https://api.twelvedata.com/"+endpoint+"?"+parameters+"&apikey="+URLEncoder.encode(apiKey,java.nio.charset.StandardCharsets.UTF_8));
            HttpResponse<String> response=client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).GET().build(),HttpResponse.BodyHandlers.ofString());
            JsonNode data=mapper.readTree(response.body());
            if(response.statusCode()!=200 || "error".equals(data.path("status").asText()))throw fail(HttpStatus.SERVICE_UNAVAILABLE,"Market data provider could not serve this instrument. Check API access and quota.");
            return data;
        }catch(ResponseStatusException e){throw e;}catch(InterruptedException e){Thread.currentThread().interrupt();throw fail(HttpStatus.SERVICE_UNAVAILABLE,"Market data request interrupted");}
        catch(Exception e){throw fail(HttpStatus.SERVICE_UNAVAILABLE,"Market data is temporarily unavailable");}
    }
    public synchronized Quote quote(String symbol){
        symbol(symbol);Quote cached=quotes.get(symbol);
        if(cached!=null && cached.getExpiresAt().isAfter(Instant.now()))return cached;
        JsonNode data=fetch("quote","symbol="+symbol+"&timezone=UTC");
        if(!"USD".equals(data.path("currency").asText()))throw fail(HttpStatus.SERVICE_UNAVAILABLE,"Unexpected instrument currency");
        if(fxAt.plusSeconds(60).isBefore(Instant.now())){fx=fetch("exchange_rate","symbol=USD%2FEUR");fxAt=Instant.now();}
        try{
            Quote q=new Quote();q.setId(UUID.randomUUID().toString());q.setSymbol(symbol);q.setPrice(new BigDecimal(data.path("close").asText()));
            q.setExchangeRate(new BigDecimal(fx.path("rate").asText()));q.setExchangeRateAt(Instant.ofEpochSecond(fx.path("timestamp").asLong()));
            q.setEurPrice(q.getPrice().multiply(q.getExchangeRate()).setScale(6,RoundingMode.HALF_UP));q.setChange(new BigDecimal(data.path("percent_change").asText("0")));
            q.setPriceAt(Instant.ofEpochSecond(data.path("timestamp").asLong()));q.setMarketOpen(data.path("is_market_open").asBoolean());q.setFetchedAt(Instant.now());q.setExpiresAt(q.getFetchedAt().plusSeconds(60));
            if(q.getPrice().signum()<=0 || q.getExchangeRate().signum()<=0 || q.getPriceAt().isAfter(Instant.now().plusSeconds(60)) || q.getExchangeRateAt().isAfter(Instant.now().plusSeconds(60)) || q.getPriceAt().isBefore(Instant.now().minusSeconds(q.isMarketOpen()?300:5*86400)) || q.getExchangeRateAt().isBefore(Instant.now().minusSeconds(5*86400)))throw new IllegalArgumentException();
            quotes.put(symbol,q);return q;
        }catch(Exception e){throw fail(HttpStatus.SERVICE_UNAVAILABLE,"Provider returned an invalid or stale price. Orders are unavailable.");}
    }
    public synchronized Quote accepted(String symbol,String id){
        Quote q=quotes.get(symbol);
        if(q==null || !q.getId().equals(id) || !q.getExpiresAt().isAfter(Instant.now()))throw fail(HttpStatus.CONFLICT,"Quote expired. Refresh the price and review the order again.");
        return q;
    }
    public synchronized JsonNode chart(String symbol,String range){
        symbol(symbol);String interval;int count;
        switch(range){case "1D":interval="5min";count=78;break;case "1W":interval="1h";count=40;break;case "1M":interval="1day";count=22;break;case "1Y":interval="1day";count=252;break;default:throw fail(HttpStatus.BAD_REQUEST,"Choose 1D, 1W, 1M or 1Y");}
        String cacheKey=symbol+range;
        if(chartTimes.containsKey(cacheKey) && chartTimes.get(cacheKey).plusSeconds(60).isAfter(Instant.now()))return charts.get(cacheKey);
        JsonNode data=fetch("time_series","symbol="+symbol+"&interval="+interval+"&outputsize="+count+"&timezone=UTC");
        if(!data.path("values").isArray() || data.path("values").isEmpty())throw fail(HttpStatus.SERVICE_UNAVAILABLE,"No chart data available");
        charts.put(cacheKey,data);chartTimes.put(cacheKey,Instant.now());return data;
    }
    private static ResponseStatusException fail(HttpStatus s,String m){return new ResponseStatusException(s,m);}
}
