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

        if (accountModel.getDailyTxSum() + amount > accountModel.getDailyTxSumLimit()) {
            return ValidationResults.success(true, ResponseCode.DAILY_TX_SUM_EXCEEDED, "Daily tx amount limit exceeded");
        }

        if (accountModel.getDailyTx() >= accountModel.getDailyTxLimit()) {
            return ValidationResults.success(true, ResponseCode.DAILY_TX_LIMIT_EXCEEDED, "Daily tx frequency limit exceeded");
        }

        if (accountModel.getAmount() < amount) {
            return ValidationResults.success(true, ResponseCode.NOT_SUFFICIENT_FUNDS, "Insufficient funds");
        }

        return ValidationResults.success(false, ResponseCode.APPROVED, "Successful Payment");
    }
}
