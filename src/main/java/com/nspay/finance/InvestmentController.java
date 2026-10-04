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
    private final InvestmentService investments;private final MarketDataService market;
    public InvestmentController(InvestmentService i,MarketDataService m){investments=i;market=m;}
    private long id(HttpServletRequest r){Object id=r.getAttribute("authenticatedUserId");if(!(id instanceof Number))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);return ((Number)id).longValue();}
    @GetMapping @io.swagger.v3.oas.annotations.Operation(summary="Get investment cash, holdings history and instrument catalog", description="Cash is denominated in EUR. Holdings are the net BUY and SELL quantities in order history. configured indicates whether a server market-data API key is present.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200",description="Successful response",content=@io.swagger.v3.oas.annotations.media.Content(mediaType="application/json",schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=com.nspay.config.ApiViews.Portfolio.class)))
    public Map<String,Object> portfolio(HttpServletRequest r){return investments.portfolio(id(r));}
    @GetMapping("/{symbol}/quote") @io.swagger.v3.oas.annotations.Operation(summary="Get a stock quote and USD/EUR conversion", description="Fetches Twelve Data prices and a USD/EUR rate. Quotes include market-open state, provider timestamps, a server quote ID and a 60-second expiry. Closed markets use the disclosed last available price. Missing credentials, invalid data or provider errors return 503; API quota limits return 429.")
    public MarketDataService.Quote quote(HttpServletRequest r,@PathVariable String symbol){id(r);return market.quote(symbol);}
    @GetMapping("/{symbol}/chart") @io.swagger.v3.oas.annotations.Operation(summary="Get historical stock prices", description="range accepts 1D, 1W, 1M or 1Y. Returns Twelve Data time-series metadata and OHLC values, newest first, with UTC timestamps. Data is cached for 60 seconds.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200",description="Successful response",content=@io.swagger.v3.oas.annotations.media.Content(mediaType="application/json",schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=com.nspay.config.ApiViews.TimeSeries.class)))
    public JsonNode chart(HttpServletRequest r,@PathVariable String symbol,@RequestParam(defaultValue="1M") String range){id(r);return market.chart(symbol,range);}
    @PostMapping("/deposit") @io.swagger.v3.oas.annotations.Operation(summary="Move account funds into investment cash", description="Atomically debits the active main account and credits EUR investment cash. requestKey must identify this operation uniquely. Retrying the same key and amount does not move funds twice; conflicting contents return 409.")
    public InvestmentWallet deposit(HttpServletRequest r,@Valid @RequestBody InvestmentService.Funding body){return investments.fund(id(r),body,false);}
    @PostMapping("/withdraw") @io.swagger.v3.oas.annotations.Operation(summary="Return investment cash to the main account", description="Atomically debits available investment cash and credits the active main account. Held shares must be sold first. requestKey provides duplicate-request protection.")
    public InvestmentWallet withdraw(HttpServletRequest r,@Valid @RequestBody InvestmentService.Funding body){return investments.fund(id(r),body,true);}
    @PostMapping("/orders") @io.swagger.v3.oas.annotations.Operation(summary="Buy or sell app-managed shares", description="Use quoteId from the current quote endpoint. The server computes EUR value from price and FX; client prices are ignored. BUY debits investment cash. SELL requires sufficient owned shares and credits investment cash. Cash, order and request-key record commit atomically. An expired quote returns 409. No broker order is placed.")
    public TradeOrder order(HttpServletRequest r,@Valid @RequestBody InvestmentService.Order body){return investments.order(id(r),body);}
}
