package com.nspay.validators;

import com.nspay.dto.TransactionDTO;
import org.springframework.stereotype.Component;

@Component
public class TransactionValidator {

    private final StructuralValidator structuralValidator;
    private final BusinessValidator businessValidator;

    public TransactionValidator(StructuralValidator structuralValidator, BusinessValidator businessValidator) {
        this.structuralValidator = structuralValidator;
        this.businessValidator = businessValidator;
    }

    public ValidationResults validate(TransactionDTO transactionDTO) {
        ValidationResults structuralResults = structuralValidator.validate(transactionDTO);
        if (!structuralResults.isValid()) {
            return structuralResults;
        }
        return businessValidator.validate(transactionDTO);
    }
}
