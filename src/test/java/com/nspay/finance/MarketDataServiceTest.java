package com.nspay.finance;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MarketDataServiceTest {
    ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
    MarketQuoteRepository repository;MarketDataService service;
    Map<String,MarketQuote> stored;JsonNode latest,daily;
    @BeforeEach void setup() throws Exception {
        repository=mock(MarketQuoteRepository.class);stored=new ConcurrentHashMap<>();
        when(repository.findByProviderAndExpiresAtAfter(anyString(),any())).thenAnswer(i -> {
            List<MarketQuote> found=new ArrayList<>();for(MarketQuote q:stored.values())if(q.getProvider().equals(i.getArgument(0))&&q.getExpiresAt().isAfter(i.getArgument(1)))found.add(q);return found;});
        when(repository.save(any())).thenAnswer(i -> {MarketQuote q=i.getArgument(0);stored.put(q.getId(),q);return q;});
        when(repository.findById(anyString())).thenAnswer(i -> Optional.ofNullable(stored.get(i.getArgument(0))));
        when(repository.saveAll(any())).thenAnswer(i -> {List<MarketQuote> rows=new ArrayList<>();for(MarketQuote q:(Iterable<MarketQuote>)i.getArgument(0)){stored.put(q.getId(),q);rows.add(q);}return rows;});
        service=spy(instance("42"));ObjectNode l=mapper.createObjectNode(),d=mapper.createObjectNode();ObjectNode lb=l.putObject("bars"),db=d.putObject("bars");
        for(MarketDataService.Instrument instrument:service.catalog()){
            lb.putObject(instrument.getSymbol()).put("c",100).put("t",Instant.now().minusSeconds(60).toString());
            db.putArray(instrument.getSymbol()).addObject().put("c",90).put("t",Instant.now().minusSeconds(86400*2).toString());
        }
        latest=l;daily=d;doAnswer(i -> i.getArgument(0).equals("v2/stocks/bars/latest")?latest:daily).when(service).fetch(anyString(),anyString());
    }
    MarketDataService instance(String seed){return new MarketDataService("cuckootrade","","https://cuckootrade.com/api/v1/alpaca",seed,1,new BigDecimal("0.92"),mapper,repository);}
    @Test void batchedQuotesCoalesceAndWorkAcrossInstances() throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(4);
        try{List<Callable<MarketDataService.Quote>> calls=Arrays.asList(()->service.quote("AAPL"),()->service.quote("SPY"),()->service.quote("MSFT"),()->service.quote("AAPL"));for(Future<MarketDataService.Quote> f:pool.invokeAll(calls))assertNotNull(f.get().getId());}finally{pool.shutdownNow();}
        verify(service,times(2)).fetch(anyString(),anyString());assertEquals(service.catalog().size(),stored.size());
        MarketDataService.Quote q=service.quote("AAPL");assertEquals(new BigDecimal("92.000000"),q.getEurPrice());assertTrue(q.isSynthetic());assertEquals("Configured sample rate",q.getExchangeRateSource());
        assertEquals(q.getId(),instance("42").accepted("AAPL",q.getId()).getId());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->instance("other").accepted("AAPL",q.getId()));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.accepted("MSFT",q.getId()));
    }
    @Test void refreshDoesNotInvalidateAnUnexpiredReviewedQuote() {
        MarketDataService.Quote q=service.quote("AAPL");
        // A second persisted quote can coexist, such as another replica's overlapping refresh.
        MarketQuote duplicate=new MarketQuote();duplicate.setId(UUID.randomUUID().toString());duplicate.setSymbol("AAPL");duplicate.setProvider(service.source());duplicate.setPayload(stored.get(q.getId()).getPayload());duplicate.setExpiresAt(Instant.now().plusSeconds(60));stored.put(duplicate.getId(),duplicate);
        assertEquals(q.getId(),service.accepted("AAPL",q.getId()).getId());
        stored.get(q.getId()).setExpiresAt(Instant.now().minusSeconds(1));
        org.springframework.web.server.ResponseStatusException e=assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.accepted("AAPL",q.getId()));assertEquals(409,e.getRawStatusCode());
    }
    @Test void invalidProviderPricesNeverCreateExecutableQuotes() {
        ((ObjectNode)latest.path("bars").path("AAPL")).put("c",-1);
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.quote("AAPL"));assertTrue(stored.isEmpty());
    }
    @Test void liveQuotesUseProviderTicksAndKeepReviewedPricesImmutable() {
        CuckooStreamFeed feed=new CuckooStreamFeed(mapper,"cuckootrade",true,"demo","https://cuckootrade.com/api/v1/stream","42",1);
        org.springframework.test.util.ReflectionTestUtils.setField(service,"stream",feed);
        try{
            feed.accept(mapper.createObjectNode().put("S","AAPL").put("p","100").put("t",Instant.now().minusSeconds(1).toString()));
            MarketDataService.Quote first=service.quote("AAPL");assertEquals("demo",first.getClock());assertEquals(new BigDecimal("92.000000"),first.getEurPrice());
            feed.accept(mapper.createObjectNode().put("S","AAPL").put("p","110").put("t",Instant.now().toString()));
            MarketDataService.Quote second=service.quote("AAPL");assertNotEquals(first.getId(),second.getId());assertEquals(new BigDecimal("101.200000"),second.getEurPrice());
            assertEquals(new BigDecimal("92.000000"),service.accepted("AAPL",first.getId()).getEurPrice());
            assertEquals(2,service.chart("AAPL","LIVE").path("values").size());verify(service,never()).fetch(anyString(),anyString());
            org.springframework.test.util.ReflectionTestUtils.setField(service,"stream",null);
            assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.accepted("AAPL",first.getId()));
        }finally{feed.stop();}
    }
    @Test void chartNormalizesOHLCAndCachesTheAllowedRange() {
        ObjectNode response=mapper.createObjectNode();response.putObject("bars").putArray("SPY").addObject().put("c",600).put("o",590).put("h",605).put("l",585).put("v",10000).put("t",Instant.now().minusSeconds(60).toString());
        doReturn(response).when(service).fetch(eq("v2/stocks/bars"),anyString());
        JsonNode chart=service.chart("SPY","1M");assertEquals("USD",chart.path("meta").path("currency").asText());assertEquals("600",chart.path("values").get(0).path("close").asText());assertTrue(chart.path("synthetic").asBoolean());
        assertSame(chart,service.chart("SPY","1M"));verify(service,times(1)).fetch(anyString(),anyString());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.chart("SPY","ALL"));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.quote("UNLISTED"));
    }
}
