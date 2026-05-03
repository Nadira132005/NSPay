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
            return ValidationResults.failure(ResponseCode.INVALID_CARD_NUMBER, "Account not found");
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
