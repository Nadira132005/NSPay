package com.aciworldwide.aclabs26.controllers;

import com.aciworldwide.aclabs26.dto.TransactionDTO;
import com.aciworldwide.aclabs26.services.TransactionService;
import com.aciworldwide.aclabs26.validators.ValidationResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/transaction")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @PostMapping
    public ResponseEntity<?> processTransaction(@RequestBody TransactionDTO transactionDTO) {
        ValidationResults validationResults = transactionService.processTransaction(transactionDTO);
        if(!validationResults.isValid()){
            return ResponseEntity.badRequest().body(validationResults);
        }
        return ResponseEntity.ok().body(validationResults);
    }

}
