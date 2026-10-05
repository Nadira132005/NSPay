package com.nspay.finance;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import com.sun.net.httpserver.HttpServer;
import static org.junit.jupiter.api.Assertions.*;
class CuckooStreamFeedTest {
    ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
    @Test void reconnectsAfterProviderClosesConnectionAndReceivesFreshPrices() throws Exception {
        AtomicInteger connections=new AtomicInteger();
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/stream",exchange -> {
            boolean apple=exchange.getRequestURI().getQuery().startsWith("symbols=AAPL");
            int sequence=apple?connections.incrementAndGet():1;
            String event="event: tick\ndata: {\"S\":\""+(apple?"AAPL":"QQQ")+"\",\"p\":"+(100+sequence)+",\"t\":\""+Instant.now()+"\"}\n\n";
            byte[] bytes=event.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","text/event-stream");exchange.sendResponseHeaders(200,bytes.length);
            try(java.io.OutputStream body=exchange.getResponseBody()){body.write(bytes);}
            exchange.close();
        });
        CuckooStreamFeed feed=new CuckooStreamFeed(mapper,"cuckootrade",true,"demo","http://127.0.0.1:"+server.getAddress().getPort()+"/stream","42",1);
        server.start();
        try{
            feed.start();long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(6);
            while(feed.history("AAPL").size()<2&&System.nanoTime()<deadline)Thread.sleep(25);
            assertTrue(feed.history("AAPL").size()>=2,"Closed provider streams must reconnect");
            assertNotNull(feed.current("QQQ"));assertTrue(feed.current("AAPL").getPrice().intValue()>=102);
        }finally{feed.stop();server.stop(0);}
    }
    @Test void rejectsMalformedStaleFutureAndOutOfOrderTicksAndBoundsHistory(){
        CuckooStreamFeed feed=new CuckooStreamFeed(mapper,"cuckootrade",true,"demo","https://cuckootrade.com/api/v1/stream","42",1);
        try{
            feed.accept(mapper.createObjectNode().put("S","AAPL").put("p",100).put("t",Instant.now().minusSeconds(30).toString()));assertNull(feed.current("AAPL"));
            feed.accept(mapper.createObjectNode().put("S","AAPL").put("p",-1).put("t",Instant.now().toString()));assertNull(feed.current("AAPL"));
            feed.accept(mapper.createObjectNode().put("S","AAPL").put("p",100).put("t",Instant.now().plusSeconds(60).toString()));assertNull(feed.current("AAPL"));
            Instant first=Instant.now().minusSeconds(8);
            for(int i=0;i<605;i++)feed.accept(mapper.createObjectNode().put("S","AAPL").put("p",100+i*.01).put("t",first.plusMillis(i*10).toString()));
            assertEquals(600,feed.history("AAPL").size());String last=feed.current("AAPL").getPrice().toString();
            feed.accept(mapper.createObjectNode().put("S","AAPL").put("p",5).put("t",first.toString()));assertEquals(last,feed.current("AAPL").getPrice().toString());
        }finally{feed.stop();}
    }
}
