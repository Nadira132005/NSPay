package com.nspay.finance;

import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import io.swagger.v3.oas.annotations.Operation;

@RestController @RequestMapping("/api/account-limits") @io.swagger.v3.oas.annotations.tags.Tag(name="Account limits")
public class AccountLimitsController {
    private final AccountLimitsService service;public AccountLimitsController(AccountLimitsService service){this.service=service;}
    private long actor(HttpServletRequest r){Object id=r.getAttribute("authenticatedUserId");if(!(id instanceof Number))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);return ((Number)id).longValue();}
    @GetMapping @Operation(summary="List accounts and daily payment usage",description="Stored administrator role required. Masked cards, server search/status filters, whitelisted sorting and pagination (maximum 100). Usage derives from successful card payments and outgoing transfers in Europe/Bucharest; incoming and internal savings/investment movements are excluded. Thresholds are advisory and never reject payments or change account status.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200",description="Paged account limits and usage",content=@io.swagger.v3.oas.annotations.media.Content(schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=com.nspay.config.ApiViews.AccountLimitQueue.class)))
    public Map<String,Object> list(HttpServletRequest r,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="") String search,@RequestParam(defaultValue="All") String status,@RequestParam(defaultValue="cardHolderName") String sort,@RequestParam(defaultValue="asc") String direction){return service.list(actor(r),page,size,search,status,sort,direction);}
    @PutMapping("/{accountId}") @Operation(summary="Change an account’s advisory daily limits",description="Administrator only. Accepts count and EUR sum limits only; balances, usage, identity and status cannot be modified here. Positive limits required. Account lock, limits, open payment scores and audit commit atomically. Customer warnings start at 80% usage, reached at 100%, exceeded above 100%. Existing reviewed decisions retain their snapshots.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200",description="Updated account limits and usage",content=@io.swagger.v3.oas.annotations.media.Content(schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=com.nspay.config.ApiViews.AccountLimitEntry.class)))
    public Map<String,Object> update(HttpServletRequest r,@PathVariable long accountId,@Valid @RequestBody AccountLimitsService.Limits request){return service.update(actor(r),accountId,request);}
}
