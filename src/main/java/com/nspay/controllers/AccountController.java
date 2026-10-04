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
    @io.swagger.v3.oas.annotations.Operation(summary="Update account details and limits", description="Administrator only. This endpoint cannot change status; use the dedicated security status endpoint.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200",description="Successful response",content=@io.swagger.v3.oas.annotations.media.Content(mediaType="application/json",schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=com.nspay.entities.AccountModel.class)))
    public ResponseEntity<?> updateAccount(@PathVariable long id,
                                           @Valid @RequestBody AccountDTO accountDTO) {
        logger.info("PUT /accounts/{} called", id);
        return accountService.updateAccount(id, accountDTO);
    }

    @DeleteMapping("/{id}")
    @io.swagger.v3.oas.annotations.Operation(summary="Delete an account", description="Administrator only.")
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
