package com.aciworldwide.aclabs26.validators;

import com.aciworldwide.aclabs26.dto.TransactionDTO;
import com.aciworldwide.aclabs26.entities.AccountModel;
import com.aciworldwide.aclabs26.repository.AccountRepository;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;

@Component
public class TransactionValidator {

    private final AccountRepository accountRepository;

    public TransactionValidator(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public ValidationResults validate(TransactionDTO transactionDTO) {
        String cardNumber = transactionDTO.getCardNumber();
        Double amount = transactionDTO.getAmount();
        Timestamp timestamp = transactionDTO.getTimestamp();

        AccountModel accountModel = accountRepository.findAccountByCardNumber(cardNumber);

        if (accountModel==null){
            return ValidationResults.failure("99", "Account not found");
        }

        if (amount == null){
            return ValidationResults.failure("100", "Amount is null");
        }

        if (timestamp == null){
            return ValidationResults.failure("101", "Timestamp is null");
        }

        if (accountModel.dailyTxLimitExceeded()){
            return ValidationResults.success(true, "61", "Daily tx limit exceeded");
        }

        if (accountModel.getAmount() < transactionDTO.getAmount()){
            return ValidationResults.success(true, "51", "Insufficient funds");
        }

        return ValidationResults.success(false, "00", "Successful Payment");

    }
}
