package com.nspay.validators;

import com.nspay.dto.TransactionDTO;
import org.springframework.stereotype.Component;

@Component
public class StructuralValidator {

    public ValidationResults validate(TransactionDTO transactionDTO) {
        if (transactionDTO == null) {
            return ValidationResults.failure(ResponseCode.INVALID_CARD_NUMBER, "Transaction is required");
        }

        if (transactionDTO.getCardNumber() == null || transactionDTO.getCardNumber().trim().isEmpty()) {
            return ValidationResults.failure(ResponseCode.INVALID_CARD_NUMBER, "Card number is required");
        }

        if (transactionDTO.getAmount() == null) {
            return ValidationResults.failure(ResponseCode.INVALID_CARD_NUMBER, "Amount is required");
        }

        if (transactionDTO.getAmount() <= 0) {
            return ValidationResults.failure(ResponseCode.INVALID_CARD_NUMBER, "Amount must be greater than zero");
        }

        if (transactionDTO.getTimestamp() == null) {
            return ValidationResults.failure(ResponseCode.INVALID_CARD_NUMBER, "Timestamp is required");
        }

        return ValidationResults.success(false, ResponseCode.APPROVED, "Structural validation passed");
    }
}
