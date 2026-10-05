package com.nspay.validators;

import com.nspay.dto.TransactionDTO;
import com.nspay.entities.AccountModel;
import com.nspay.entities.AccountStatus;
import com.nspay.repository.AccountRepository;
import org.springframework.stereotype.Component;

@Component
public class BusinessValidator {

    private final AccountRepository accountRepository;

    public BusinessValidator(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public ValidationResults validate(TransactionDTO transactionDTO) {
        AccountModel accountModel = accountRepository.findByCardNumber(transactionDTO.getCardNumber());
        if (accountModel == null) {
            return ValidationResults.failure(ResponseCode.INVALID_CARD_NUMBER, "Account not found");
        }

        if (accountModel.getStatus() != AccountStatus.ACTIVE) {
            return ValidationResults.failure(ResponseCode.INACTIVE_ACCOUNT, "Account is not active");
        }

        Double amount = transactionDTO.getAmount();
        if (amount == null) {
            return ValidationResults.failure(ResponseCode.INVALID_CARD_NUMBER, "Amount is null");
        }

        if (accountModel.getAmount() < amount) {
            return ValidationResults.success(true, ResponseCode.NOT_SUFFICIENT_FUNDS, "Insufficient funds");
        }

        return ValidationResults.success(false, ResponseCode.APPROVED, "Successful Payment");
    }
}
