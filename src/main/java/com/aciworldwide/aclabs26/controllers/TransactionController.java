package com.aciworldwide.aclabs26.controllers;

import com.aciworldwide.aclabs26.dto.TransactionDTO;
import com.aciworldwide.aclabs26.entities.TransactionModel;
import com.aciworldwide.aclabs26.services.TransactionService;
import com.aciworldwide.aclabs26.validators.ValidationResults;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@Tag(name = "Transactions", description = "Operations related to transactions")
public class TransactionController {

    private static final Logger logger = LogManager.getLogger(TransactionController.class);

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping({"/transaction", "/api/accounts/transaction"})
    @Operation(summary = "Process a transaction")
    public ResponseEntity<?> processTransaction(@Valid @RequestBody TransactionDTO transactionDTO) {
        logger.info("Transaction request received for card {}", transactionDTO.getCardNumber());
        ValidationResults validationResults = transactionService.processTransaction(transactionDTO);
        if (!validationResults.isValid()) {
            return ResponseEntity.badRequest().body(validationResults);
        }

        return ResponseEntity.ok().body(validationResults);
    }

    @GetMapping({"/transactions", "/api/accounts/transactions", "/api/accounts/transactions/"})
    @Operation(summary = "Get all transactions")
    public ResponseEntity<List<TransactionModel>> getTransactions() {
        logger.info("GET /transactions called");
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    @GetMapping({"/transactions/{cardNumber}", "/api/accounts/transactions/{cardNumber}", "/api/accounts/transactions/{cardNumber}/"})
    @Operation(summary = "Get transactions by card number")
    public ResponseEntity<List<TransactionModel>> getTransactionsOfAccount(@PathVariable String cardNumber) {
        logger.info("GET /transactions/{} called", cardNumber);
        return ResponseEntity.ok(transactionService.getTransactionsOfAccount(cardNumber));
    }
}
