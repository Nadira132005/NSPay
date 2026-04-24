package com.aciworldwide.aclabs26.controllers;

import com.aciworldwide.aclabs26.dto.TransactionDTO;
import com.aciworldwide.aclabs26.exceptions.ValidationException;
import com.aciworldwide.aclabs26.services.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import javax.validation.Valid;

@RestController
@RequestMapping(path = "/transaction")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<?> processTransaction(@Valid @RequestBody TransactionDTO transactionDTO,
                                                BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String message = bindingResult.getFieldErrors()
                    .stream()
                    .map(this::formatFieldError)
                    .reduce((left, right) -> left + "; " + right)
                    .orElse("Invalid transaction");
            throw new ValidationException(message);
        }
        return ResponseEntity.ok("transaction passed successfully");
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
