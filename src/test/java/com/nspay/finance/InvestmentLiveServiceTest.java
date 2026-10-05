package com.nspay.finance;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class InvestmentLiveServiceTest {
    BigDecimal n(String value){return new BigDecimal(value);}
    @Test void pricesChangeValuationWithoutChangingCashBasisOrRealizedGains(){
        Map<String,Object> holding=new LinkedHashMap<>();holding.put("symbol","AAPL");holding.put("provider","source");holding.put("quantity",n("2"));holding.put("costBasis",n("180"));holding.put("accountingBasis",n("180"));
        Map<String,Object> base=new LinkedHashMap<>();base.put("holdings",Collections.singletonList(holding));base.put("cash",n("50"));base.put("realizedGain",n("10"));base.put("liveExchangeRate",n("0.9"));
        Map<String,CuckooStreamFeed.Tick> ticks=Collections.singletonMap("AAPL",new CuckooStreamFeed.Tick("AAPL",n("110"),Instant.now(),n("1")));
        Map<String,Object> view=InvestmentLiveService.mark(base,ticks,"source");assertEquals(n("248.00"),view.get("totalValue"));assertEquals(n("28.00"),view.get("totalGain"));assertEquals(n("50"),view.get("cash"));assertEquals(n("10"),view.get("realizedGain"));assertFalse(holding.containsKey("marketValue"));
        Map<String,Object> stale=InvestmentLiveService.mark(base,Collections.emptyMap(),"source");assertEquals("INCOMPLETE",stale.get("valuationStatus"));assertNull(stale.get("totalValue"));assertNull(stale.get("totalGain"));assertEquals(n("50"),stale.get("cash"));
    }
}
