package com.aciworldwide.aclabs26.validators;

import com.aciworldwide.aclabs26.dto.TransactionDTO;
import org.springframework.stereotype.Component;

@Component
public class StructuralValidator {

    public ValidationResults validate(TransactionDTO transactionDTO) {
        if (transactionDTO == null) {
            return ValidationResults.failure("98", "Transaction is required");
        }

        if (transactionDTO.getCardNumber() == null || transactionDTO.getCardNumber().trim().isEmpty()) {
            return ValidationResults.failure("98", "Card number is required");
        }

        if (transactionDTO.getAmount() == null) {
            return ValidationResults.failure("100", "Amount is required");
        }

        if (transactionDTO.getAmount() <= 0) {
            return ValidationResults.failure("100", "Amount must be greater than zero");
        }

        if (transactionDTO.getTimestamp() == null) {
            return ValidationResults.failure("101", "Timestamp is required");
        }

        return ValidationResults.success(false, "00", "Structural validation passed");
    }
}
