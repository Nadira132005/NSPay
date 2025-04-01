package com.aciworldwide.aclabs22.controllers;

import com.aciworldwide.aclabs22.services.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(path = "/accounts")
public class AccountController {

    @Autowired
    AccountService accountService;

    @GetMapping
    public ResponseEntity<?> getAccount() {
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
