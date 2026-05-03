package com.aciworldwide.aclabs26.validators;

import com.aciworldwide.aclabs26.dto.TransactionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ValidationLogger {
    private static final Logger logger = LoggerFactory.getLogger(ValidationLogger.class);

    public void log(TransactionDTO transactionDTO, ValidationResults validationResults) {
        String maskedCardNumber = maskCardNumber(transactionDTO.getCardNumber());
        String amount = transactionDTO.getAmount().toString();
        String returnCode = validationResults.getCode().getCode();
        String message = validationResults.getMessage();

        if (!validationResults.isValid()) {
            logger.error("Validation failed: cardNumber={}, amount={}, returnCode={}, reason={}",
                    maskedCardNumber, amount, returnCode, message);
            return;
        }

        if (validationResults.isBusinessLogicViolation()) {
            logger.warn("Validation finished with business rule violation: cardNumber={}, amount={}, returnCode={}, reason={}",
                    maskedCardNumber, amount, returnCode, message);
            return;
        }

        logger.info("Validation successful: cardNumber={}, amount={}, returnCode={}, reason={}",
                maskedCardNumber, amount, returnCode, message);
    }

    private String maskCardNumber(String cardNumber) {
        return "************" + cardNumber.substring(cardNumber.length() - 4);
    }
}
