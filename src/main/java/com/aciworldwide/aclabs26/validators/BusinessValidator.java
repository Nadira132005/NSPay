package com.aciworldwide.aclabs26.validators;

import com.aciworldwide.aclabs26.dto.TransactionDTO;
import com.aciworldwide.aclabs26.entities.AccountModel;
import com.aciworldwide.aclabs26.repository.AccountRepository;
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
            return ValidationResults.failure("99", "Account not found");
        }

        Double amount = transactionDTO.getAmount();
        if (amount == null) {
            return ValidationResults.failure("100", "Amount is null");
        }

        if (accountModel.getDailyTx() >= accountModel.getDailyTxLimit()
                || accountModel.getDailyTxSum() + amount > accountModel.getDailyTxSumLimit()) {
            return ValidationResults.success(true, "61", "Daily tx limit exceeded");
        }

        if (accountModel.getAmount() < amount) {
            return ValidationResults.success(true, "51", "Insufficient funds");
        }

        return ValidationResults.success(false, "00", "Successful Payment");
    }
}
