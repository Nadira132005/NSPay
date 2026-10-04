package com.nspay.controllers;

import com.nspay.dto.TransactionDTO;
import com.nspay.entities.TransactionModel;
import com.nspay.services.TransactionService;
import com.nspay.services.TransferService;
import com.nspay.dto.TransferRequestDTO;
import com.nspay.validators.ValidationResults;
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
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@io.swagger.v3.oas.annotations.tags.Tag(name="Payments and activity")
@RestController
public class TransactionController {

    private static final Logger logger = LogManager.getLogger(TransactionController.class);

    private final TransactionService transactionService;
    private final TransferService transferService;

    public TransactionController(TransactionService transactionService, TransferService transferService) {
        this.transactionService = transactionService;
        this.transferService = transferService;
    }

    @PostMapping("/api/transfers")
    @io.swagger.v3.oas.annotations.Operation(summary="Transfer money by IBAN", description="Uses the signed-in user as sender. Checks both account statuses, available funds and daily limits. Locks accounts in ID order and commits paired activity records with the debit and credit.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200",description="Successful response",content=@io.swagger.v3.oas.annotations.media.Content(mediaType="application/json",schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=com.nspay.services.TransferService.TransferReceipt.class)))
    public ResponseEntity<?> transfer(@Valid @RequestBody TransferRequestDTO request,
                                      HttpServletRequest httpRequest) {
        Number userId = (Number) httpRequest.getAttribute("authenticatedUserId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(transferService.transfer(userId.longValue(), request));
    }

    @PostMapping({"/transaction", "/api/accounts/transaction"})
    @io.swagger.v3.oas.annotations.Operation(summary="Process a card payment", description="Administrator only. Validates the card, account status, limits and available balance before recording the result.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200",description="Successful response",content=@io.swagger.v3.oas.annotations.media.Content(mediaType="application/json",schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=com.nspay.validators.ValidationResults.class)))
    public ResponseEntity<?> processTransaction(@Valid @RequestBody TransactionDTO transactionDTO) {
        logger.info("Transaction request received for card {}", transactionDTO.getCardNumber());
        ValidationResults validationResults = transactionService.processTransaction(transactionDTO);
        if (!validationResults.isValid()) {
            return ResponseEntity.badRequest().body(validationResults);
        }

        return ResponseEntity.ok().body(validationResults);
    }

    @GetMapping({"/transactions", "/api/accounts/transactions", "/api/accounts/transactions/"})
    @io.swagger.v3.oas.annotations.Operation(summary="List all transactions", description="Administrator or analyst only.")
    public ResponseEntity<List<TransactionModel>> getTransactions() {
        logger.info("GET /transactions called");
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    @GetMapping({"/transactions/{cardNumber}", "/api/accounts/transactions/{cardNumber}", "/api/accounts/transactions/{cardNumber}/"})
    @io.swagger.v3.oas.annotations.Operation(summary="Get account transaction history", description="Owner, administrator or analyst only.")
    public ResponseEntity<List<TransactionModel>> getTransactionsOfAccount(@PathVariable String cardNumber) {
        logger.info("GET /transactions/{} called", cardNumber);
        return ResponseEntity.ok(transactionService.getTransactionsOfAccount(cardNumber));
    }
}
