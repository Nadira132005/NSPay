package com.aciworldwide.aclabs26.services;

import com.aciworldwide.aclabs26.dto.TransactionDTO;
import com.aciworldwide.aclabs26.validators.TransactionValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {

    @Autowired
    private TransactionValidator transactionValidator;

    public Object processTransaction(TransactionDTO transactionDTO) {
        return transactionValidator.validate(transactionDTO);
    }

}
