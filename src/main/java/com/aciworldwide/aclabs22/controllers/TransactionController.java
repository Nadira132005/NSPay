package com.aciworldwide.aclabs22.controllers;

import com.aciworldwide.aclabs22.dto.TransactionDTO;
import com.aciworldwide.aclabs22.services.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

@RestController
@RequestMapping(path="/payment")
public class TransactionController {
    private static final Logger logger=LogManager.getLogger();

    //@Autowired
    //TransactionService transactionService;
    //test

    @PostMapping
    public ResponseEntity<?> processTransaction(@RequestBody TransactionDTO transactionDTO) {
        //logger.info("Processing transaction: " + transactionDTO.getCardNumber() + " with amount: " + transactionDTO.getAmount());
        System.out.println("Processing transaction: " + transactionDTO.getCardNumber() + " with amount: " + transactionDTO.getAmount());
        //return transactionService.processTransaction(transactionDTO);
        return null;
    }
}
