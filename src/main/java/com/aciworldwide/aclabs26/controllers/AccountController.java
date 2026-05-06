package com.aciworldwide.aclabs26.controllers;

import com.aciworldwide.aclabs26.dto.AccountDTO;
import com.aciworldwide.aclabs26.entities.AccountModel;
import com.aciworldwide.aclabs26.entities.TransactionModel;
import com.aciworldwide.aclabs26.services.AccountService;
import com.aciworldwide.aclabs26.services.TransactionService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@AllArgsConstructor
@ControllerAdvice
@RestController
@RequestMapping(path = "/api/accounts")
public class AccountController {

    AccountService accountService;
    TransactionService transactionService;

    @GetMapping
    public List<AccountModel> getAllAccounts() {
        return accountService.getAllAccounts();
    }

    @GetMapping("/transactions")
    public List<TransactionModel> getAllTransactions() {
        return transactionService.getAllTransactions();
    }

    @GetMapping("/transactions/{cardNumber}")
    public List<TransactionModel> getTransactionsOfAccount(@PathVariable String cardNumber) {
        return transactionService.getTransactionsOfAccount(cardNumber);
    }

    @GetMapping("/{cardNumber}")
    public ResponseEntity<?> getAccount(@PathVariable String cardNumber) {
        return accountService.getAccount(cardNumber);
    }

    @PostMapping
    public ResponseEntity<?> addAccount(@RequestBody AccountDTO accountDTO) {
        return accountService.addAccount(accountDTO);
    }

    @PutMapping("{cardNumber}")
    public ResponseEntity<?> updateAccount(
            @PathVariable("cardNumber") String cardNumber,
            @RequestBody AccountDTO accountDTO)    {
        return accountService.updateAccount(cardNumber, accountDTO);
    }

    @DeleteMapping
    public ResponseEntity<?> deleteAccount(@RequestBody AccountDTO accountDTO) {
        return accountService.deleteAccount(accountDTO);
    }
}
