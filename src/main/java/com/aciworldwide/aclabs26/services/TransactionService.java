package com.aciworldwide.aclabs26.services;

import com.aciworldwide.aclabs26.dto.TransactionDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {

    private static final Logger logger = LogManager.getLogger(TransactionService.class);

    public void processTransaction(TransactionDTO transactionDTO) {
        logger.info("Processing transaction for card {} and amount {}", transactionDTO.getCardNumber(), transactionDTO.getAmount());
    }

}
