package com.aciworldwide.aclabs26.validators;

import com.aciworldwide.aclabs26.dto.TransactionDTO;
import org.springframework.stereotype.Component;

@Component
public class TransactionValidator {

    public boolean validate(TransactionDTO transactionDTO) {
        if (transactionDTO.getAmount() < 0) {
            return false;
        }
        return transactionDTO.getCardNumber().length() == 16;
    }
}
