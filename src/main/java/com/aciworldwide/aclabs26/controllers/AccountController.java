package com.aciworldwide.aclabs26.controllers;

import com.aciworldwide.aclabs26.services.AccountService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(path = "/accounts")
@Tag(name = "Accounts")
public class AccountController {

    private static final Logger logger = LogManager.getLogger(AccountController.class);

    @Autowired
    AccountService accountService;

    @GetMapping
    @Operation(summary = "Get all accounts")
    public ResponseEntity<?> getAccount() {
        logger.info("GET /accounts called");
        return accountService.getAllAccounts();
    }

    public ResponseEntity<?> addAccount() {
        return accountService.addAccount();
    }

    public ResponseEntity<?> updateAccount() {
        return accountService.updateAccount();
    }

    public ResponseEntity<?> deleteAccount() {
        return accountService.deleteAccount();
    }
}
