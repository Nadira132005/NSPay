package com.aciworldwide.aclabs26.services;

import com.aciworldwide.aclabs26.dto.TransactionDTO;
import com.aciworldwide.aclabs26.entities.TransactionEntity;
import com.aciworldwide.aclabs26.repository.TransactionRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {

    private static final Logger logger = LogManager.getLogger(TransactionService.class);

    @Autowired
    private TransactionRepository transactionRepository;

    public void processTransaction(TransactionDTO transactionDTO) {
        logger.info("Processing transaction for card {} and amount {}", transactionDTO.getCardNumber(), transactionDTO.getAmount());
        transactionRepository.addTransaction(TransactionEntity.builder()
                .cardNumber(transactionDTO.getCardNumber())
                .amount(transactionDTO.getAmount())
                .build());
    }

}
