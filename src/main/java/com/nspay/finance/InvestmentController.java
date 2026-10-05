package com.nspay.finance;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
@io.swagger.v3.oas.annotations.tags.Tag(name="Investments")
@io.swagger.v3.oas.annotations.responses.ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="429",description="Market data quota exceeded; retry in one minute."),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="503",description="Market data key missing, provider unavailable or data stale.")
})
@RestController @RequestMapping("/api/investments")
public class InvestmentController {
    private final InvestmentLiveService live;private final PortfolioPerformanceService performance;private final InvestmentService investments;private final MarketDataService market;
    public InvestmentController(InvestmentService i,MarketDataService m,PortfolioPerformanceService p,InvestmentLiveService live){this.live=live;performance=p;investments=i;market=m;}
    private long id(HttpServletRequest r){Object id=r.getAttribute("authenticatedUserId");if(!(id instanceof Number))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);return ((Number)id).longValue();}
    @GetMapping @io.swagger.v3.oas.annotations.Operation(summary="Get portfolio valuation and investment performance", description="Returns owned positions, EUR cash, current holdings value, average cost, realized and unrealized gains, allocation and the latest 20 orders. Prices are batched and shared. totalValue and unrealized gains are null when any holding cannot be priced; valuationStatus is INCOMPLETE. Provider/dataset changes never silently revalue old holdings. No account locks are held while fetching external prices.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200",description="Successful response",content=@io.swagger.v3.oas.annotations.media.Content(mediaType="application/json",schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=com.nspay.config.ApiViews.Portfolio.class)))
    public Map<String,Object> portfolio(HttpServletRequest r){return investments.portfolio(id(r));}
    @GetMapping(value="/stream",produces="text/event-stream")
    @io.swagger.v3.oas.annotations.Operation(summary="Stream live stock prices and owned portfolio valuation",description="Authenticated SSE stream. prices events contain shared one-second stock ticks and only the authenticated user's marked portfolio. Browser EventSource uses the session cookie. Reconnect after closure; retry delay is three seconds. Upstream connections are shared. Slow clients skip intermediate frames. Stale prices are removed and totals become incomplete. No cash or orders change because of a price tick.")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter stream(HttpServletRequest r){return live.subscribe(id(r));}
    @GetMapping("/performance") @io.swagger.v3.oas.annotations.Operation(summary="Get owned portfolio profit history",description="Daily EUR profit snapshots, oldest first. range is 1M, 3M or 1Y. Profit combines realized gains and current unrealized gains; deposits are not gains. Today's complete valuation is refreshed first. Missing market prices do not create zero-valued history. Historical records remain available during provider outages.")
    public java.util.List<PortfolioPerformance> performance(HttpServletRequest r,@RequestParam(defaultValue="1M") String range){long user=id(r);investments.portfolio(user);return performance.history(user,range);}
    @GetMapping("/quotes") @io.swagger.v3.oas.annotations.Operation(summary="Get current prices for the instrument catalog",description="Returns 60-second executable quotes keyed by symbol. With clock=demo, quotes use the current shared SSE ticks; the REST mode batches catalog prices. Synthetic and FX-source fields disclose the configured source.")
    public Map<String,MarketDataService.Quote> quotes(HttpServletRequest r){id(r);return market.quotes(market.catalog().stream().map(MarketDataService.Instrument::getSymbol).collect(java.util.stream.Collectors.toList()));}
    @GetMapping("/orders") @io.swagger.v3.oas.annotations.Operation(summary="Get paginated owned order history",description="Returns only the authenticated user's orders, newest first. Page is zero-based; size is 1–100, default 20. Includes totalElements and totalPages.")
    public org.springframework.data.domain.Page<TradeOrder> history(HttpServletRequest r,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return investments.history(id(r),page,size);}
    @GetMapping("/{symbol}/quote") @io.swagger.v3.oas.annotations.Operation(summary="Get a stock quote and USD/EUR conversion", description="Returns the configured provider's current price. With clock=demo, uses the always-open SSE feed; other modes use the latest completed REST price. CuckooTrade supplies synthetic USD prices and a disclosed configured sample FX rate; Twelve Data supplies market prices and FX. Quotes include provenance, provider timestamps, a server quote ID and a 60-second expiry. IDs work across backend instances. Closed markets use the last available price. Invalid data or provider errors return 503; quota limits return 429.")
    public MarketDataService.Quote quote(HttpServletRequest r,@PathVariable String symbol){id(r);return market.quote(symbol);}
    @GetMapping("/{symbol}/chart") @io.swagger.v3.oas.annotations.Operation(summary="Get historical stock prices", description="range accepts LIVE, 1D, 1W, 1M or 1Y. LIVE returns the bounded recent tick history from the always-open stream. Returns normalized time-series metadata and OHLC values, newest first, with UTC timestamps. Data is cached for 60 seconds. Requests for the same chart coalesce; different charts can load concurrently.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200",description="Successful response",content=@io.swagger.v3.oas.annotations.media.Content(mediaType="application/json",schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=com.nspay.config.ApiViews.TimeSeries.class)))
    public JsonNode chart(HttpServletRequest r,@PathVariable String symbol,@RequestParam(defaultValue="1M") String range){id(r);return market.chart(symbol,range);}
    @PostMapping("/deposit") @io.swagger.v3.oas.annotations.Operation(summary="Move account funds into investment cash", description="Atomically debits the active main account and credits EUR investment cash. requestKey must identify this operation uniquely. Retrying the same key and amount does not move funds twice; conflicting contents return 409.")
    public InvestmentWallet deposit(HttpServletRequest r,@Valid @RequestBody InvestmentService.Funding body){long user=id(r);InvestmentWallet wallet=investments.fund(user,body,false);live.invalidate(user);return wallet;}
    @PostMapping("/withdraw") @io.swagger.v3.oas.annotations.Operation(summary="Return investment cash to the main account", description="Atomically debits available investment cash and credits the active main account. Held shares must be sold first. requestKey provides duplicate-request protection.")
    public InvestmentWallet withdraw(HttpServletRequest r,@Valid @RequestBody InvestmentService.Funding body){long user=id(r);InvestmentWallet wallet=investments.fund(user,body,true);live.invalidate(user);return wallet;}
    @PostMapping("/orders") @io.swagger.v3.oas.annotations.Operation(summary="Buy or sell app-managed shares", description="Use quoteId from the current quote endpoint. The server computes EUR value from price and FX; client prices are ignored. BUY debits investment cash. SELL requires sufficient owned shares and credits investment cash. Cash, order and request-key record commit atomically. An expired quote returns 409. No broker order is placed.")
    public TradeOrder order(HttpServletRequest r,@Valid @RequestBody InvestmentService.Order body){long user=id(r);TradeOrder order=investments.order(user,body);live.invalidate(user);return order;}
}
