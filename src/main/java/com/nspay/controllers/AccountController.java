package com.nspay.controllers;

import com.nspay.dto.AccountDTO;
import com.nspay.services.AccountService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;


@io.swagger.v3.oas.annotations.tags.Tag(name="Account administration")
@RestController
@RequestMapping(path = {"/accounts", "/api/accounts"})
public class AccountController {

    private static final Logger logger = LogManager.getLogger(AccountController.class);

    @Autowired
    AccountService accountService;
    @Autowired private com.nspay.finance.AccountLimitsService limits;

    @GetMapping
    @io.swagger.v3.oas.annotations.Operation(summary="List accounts", description="Administrator or analyst only.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200",description="Successful response",content=@io.swagger.v3.oas.annotations.media.Content(mediaType="application/json",array=@io.swagger.v3.oas.annotations.media.ArraySchema(schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=com.nspay.entities.AccountModel.class))))
    public ResponseEntity<?> getAccounts() {
        logger.info("GET /accounts called");
        return accountService.getAllAccounts();
    }

    @GetMapping("/{id}")
    @io.swagger.v3.oas.annotations.Operation(summary="Get an account by ID", description="Owner, administrator or analyst only.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200",description="Successful response",content=@io.swagger.v3.oas.annotations.media.Content(mediaType="application/json",schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=com.nspay.entities.AccountModel.class)))
    public ResponseEntity<?> getAccountById(@PathVariable long id) {
        logger.info("GET /accounts/{} called", id);
        return accountService.getAccountById(id);
    }

    @GetMapping("/card-number/{cardNumber}")
    @io.swagger.v3.oas.annotations.Operation(summary="Get an account by card number", description="Owner, administrator or analyst only.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200",description="Successful response",content=@io.swagger.v3.oas.annotations.media.Content(mediaType="application/json",schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=com.nspay.entities.AccountModel.class)))
    public ResponseEntity<?> getAccountByCardNumber(@PathVariable String cardNumber) {
        logger.info("GET /accounts/card-number/{} called", cardNumber);
        return accountService.getAccountByCardNumber(cardNumber);
    }

    @PostMapping
    @io.swagger.v3.oas.annotations.Operation(summary="Create an account", description="Administrator only. Card must pass Luhn validation. The new account is ACTIVE and receives an IBAN.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="201",description="Successful response",content=@io.swagger.v3.oas.annotations.media.Content(mediaType="application/json",schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=com.nspay.entities.AccountModel.class)))
    public ResponseEntity<?> addAccount(@Valid @RequestBody AccountDTO accountDTO) {
        logger.info("POST /accounts called");
        return accountService.addAccount(accountDTO);
    }

    @PutMapping("/{id}")
    @io.swagger.v3.oas.annotations.Operation(summary="Update advisory account limits (legacy)", description="Administrator only. Only the daily count and EUR sum limits are applied; balance, identity, usage and status cannot be changed. Prefer PUT /api/account-limits/{accountId} for atomic score refresh and audit.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200",description="Successful response",content=@io.swagger.v3.oas.annotations.media.Content(mediaType="application/json",schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=com.nspay.entities.AccountModel.class)))
    public ResponseEntity<?> updateAccount(@PathVariable long id,
                                           @Valid @RequestBody AccountDTO accountDTO, javax.servlet.http.HttpServletRequest request) {
        logger.info("PUT /accounts/{} called", id);
        Object actor=request.getAttribute("authenticatedUserId");
        if(!(actor instanceof Number))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED);
        com.nspay.finance.AccountLimitsService.Limits payload=new com.nspay.finance.AccountLimitsService.Limits();payload.setDailyTxLimit(accountDTO.getDailyTxLimit());payload.setDailyTxSumLimit(java.math.BigDecimal.valueOf(accountDTO.getDailyTxSumLimit()));
        if(payload.getDailyTxLimit()<1||!Double.isFinite(accountDTO.getDailyTxSumLimit())||payload.getDailyTxSumLimit().signum()<=0)throw new com.nspay.exceptions.ValidationException("Enter positive daily limits");
        return ResponseEntity.ok(limits.update(((Number)actor).longValue(),id,payload));
    }

    @DeleteMapping("/{id}")
    @io.swagger.v3.oas.annotations.Operation(summary="Retired account deletion", description="Returns 405. Accounts are retained; administrators may change limits or set restrictions through Security.")
    public ResponseEntity<?> deleteAccount(@PathVariable long id) {
        logger.info("DELETE /accounts/{} called", id);
        return accountService.deleteAccount(id);
    }

    @Deprecated
    @PutMapping("/{id}/suspend")
    @io.swagger.v3.oas.annotations.Operation(summary="Legacy indefinite suspension endpoint", description="Retired indefinite suspension operation. Use POST /api/security/users/{subject}/status with a reason and a 30–90 day duration.")
    public ResponseEntity<?> suspendAccount(@PathVariable long id) {
        logger.info("PUT /accounts/{}/suspend called", id);
        return accountService.suspendAccount(id);
    }
}
