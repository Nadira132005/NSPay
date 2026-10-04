package com.nspay.finance;
import com.nspay.entities.AccountStatus;
import lombok.Data;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
@io.swagger.v3.oas.annotations.tags.Tag(name="Security and account restrictions")
@RestController @RequestMapping("/api/security")
public class SecurityController {
    private final SecurityService security;
    public SecurityController(SecurityService s){security=s;}
    private long id(HttpServletRequest r){Object id=r.getAttribute("authenticatedUserId");if(!(id instanceof Number))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);return ((Number)id).longValue();}
    @io.swagger.v3.oas.annotations.media.Schema(name="SecurityDecision")
    @Data public static class Decision {private AccountStatus status;@io.swagger.v3.oas.annotations.media.Schema(minimum="30",maximum="90",example="30",description="Required when suspending or confirming misconduct.") private Integer days;@io.swagger.v3.oas.annotations.media.Schema(minLength=5,maxLength=250,example="Confirmed unauthorized account activity") private String reason;private boolean confirmed;}
    @GetMapping @io.swagger.v3.oas.annotations.Operation(summary="Get the current user’s reports and status history", description="Returns suspension count, expiry, reason, owned security reports and status-change history.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200",description="Successful response",content=@io.swagger.v3.oas.annotations.media.Content(mediaType="application/json",schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=com.nspay.config.ApiViews.SecurityState.class)))
    public Map<String,Object> own(HttpServletRequest r){return security.own(id(r));}
    @GetMapping("/users") @io.swagger.v3.oas.annotations.Operation(summary="Get the administrator user directory", description="Administrator only. Returns account status, balance and suspension metadata. Card numbers are masked on the server to their last four digits.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200",description="Successful response",content=@io.swagger.v3.oas.annotations.media.Content(mediaType="application/json",array=@io.swagger.v3.oas.annotations.media.ArraySchema(schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=com.nspay.config.ApiViews.DirectoryEntry.class))))
    public List<Map<String,Object>> users(HttpServletRequest r){return security.directory(id(r));}
    @GetMapping("/reports") @io.swagger.v3.oas.annotations.Operation(summary="Get the security review queue", description="Administrator only. Returns the latest 100 security reports.")
    public List<SecurityReport> reports(HttpServletRequest r){return security.queue(id(r));}
    @PostMapping("/transactions/{transaction}/report") @io.swagger.v3.oas.annotations.Operation(summary="Report a suspicious transaction", description="Customer: owned transactions only. Administrator: any account transaction with an assigned user. A first report creates a case and changes an ACTIVE account to FREEZE in one transaction. Existing restrictions are retained. A repeat report for the same transaction returns its existing case.")
    public SecurityReport report(HttpServletRequest r,@PathVariable long transaction,@RequestBody Decision d){return security.report(id(r),transaction,d.getReason());}
    @PostMapping("/users/{subject}/status") @io.swagger.v3.oas.annotations.Operation(summary="Set a user’s account status", description="Administrator only. Allowed statuses: ACTIVE, FREEZE, SUSPENDED, BLOCKED. A reason of 5–250 characters is required. SUSPENDED requires 30–90 days. A second suspension becomes BLOCKED automatically. Explicitly restoring ACTIVE does not reset the count. Status, restriction metadata and history commit atomically.")
    public AccountRestriction status(HttpServletRequest r,@PathVariable long subject,@RequestBody Decision d){return security.setStatus(id(r),subject,d.getStatus(),d.getDays(),d.getReason());}
    @PostMapping("/reports/{report}/review") @io.swagger.v3.oas.annotations.Operation(summary="Review a suspicious transaction report", description="Administrator only. confirmed=true imposes a 30–90 day suspension; a second distinct confirmed case blocks the account, including during an existing suspension. confirmed=false clears the case and restores a frozen account only if no other open report remains. Repeated reviews return 409. Existing blocks are retained.")
    public SecurityReport review(HttpServletRequest r,@PathVariable long report,@RequestBody Decision d){return security.review(id(r),report,d.isConfirmed(),d.getDays(),d.getReason());}
}
