package com.aciworldwide.aclabs26.controllers;

import com.aciworldwide.aclabs26.dto.AccountDTO;
import com.aciworldwide.aclabs26.services.AccountService;
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


@RestController
@RequestMapping(path = {"/accounts", "/api/accounts"})
@Tag(name = "Accounts")
public class AccountController {

    private static final Logger logger = LogManager.getLogger(AccountController.class);

    @Autowired
    AccountService accountService;

    @GetMapping
    @Operation(summary = "Get all accounts")
    public ResponseEntity<?> getAccounts() {
        logger.info("GET /accounts called");
        return accountService.getAllAccounts();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get account by id")
    public ResponseEntity<?> getAccountById(@PathVariable long id) {
        logger.info("GET /accounts/{} called", id);
        return accountService.getAccountById(id);
    }

    @GetMapping("/card-number/{cardNumber}")
    @Operation(summary = "Get account by card number")
    public ResponseEntity<?> getAccountByCardNumber(@PathVariable String cardNumber) {
        logger.info("GET /accounts/card-number/{} called", cardNumber);
        return accountService.getAccountByCardNumber(cardNumber);
    }

    @PostMapping
    @Operation(summary = "Add account")
    public ResponseEntity<?> addAccount(@Valid @RequestBody AccountDTO accountDTO) {
        logger.info("POST /accounts called");
        return accountService.addAccount(accountDTO);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update account")
    public ResponseEntity<?> updateAccount(@PathVariable long id,
                                           @Valid @RequestBody AccountDTO accountDTO) {
        logger.info("PUT /accounts/{} called", id);
        return accountService.updateAccount(id, accountDTO);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete account")
    public ResponseEntity<?> deleteAccount(@PathVariable long id) {
        logger.info("DELETE /accounts/{} called", id);
        return accountService.deleteAccount(id);
    }

    @PutMapping("/{id}/suspend")
    @Operation(summary = "Suspend account")
    public ResponseEntity<?> suspendAccount(@PathVariable long id) {
        logger.info("PUT /accounts/{}/suspend called", id);
        return accountService.suspendAccount(id);
    }
}
