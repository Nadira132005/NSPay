package com.aciworldwide.aclabs26.services;

import com.aciworldwide.aclabs26.dto.TransactionDTO;
import com.aciworldwide.aclabs26.entities.AccountModel;
import com.aciworldwide.aclabs26.entities.TransactionModel;
import com.aciworldwide.aclabs26.repository.AccountRepository;
import com.aciworldwide.aclabs26.repository.TransactionRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.aciworldwide.aclabs26.validators.TransactionValidator;
import com.aciworldwide.aclabs26.validators.ValidationResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {

    private static final Logger logger = LogManager.getLogger(TransactionService.class);

    @Autowired
    private TransactionValidator transactionValidator;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private AccountRepository accountRepository;

    public ValidationResults processTransaction(TransactionDTO transactionDTO) {

        logger.info("Processing transaction for card {} and amount {}", transactionDTO.getCardNumber(), transactionDTO.getAmount());
        ValidationResults validationResults = transactionValidator.validate(transactionDTO);
        if (!validationResults.isValid()) {
            return validationResults;
        }

        TransactionModel transactionModel = new TransactionModel(transactionDTO);
        transactionModel.setReturnCode(validationResults.getCode().getCode());//ResponseCode.NOT_SUFFICIENT_FUNDS.getCode()->"51"
        transactionRepository.save(transactionModel);
        if(validationResults.isBusinessLogicViolation()){
            return validationResults;
        }

        AccountModel accountModel = accountRepository.findByCardNumber(transactionDTO.getCardNumber());
        updateAccount(accountModel, transactionDTO.getAmount());
        return validationResults;

    }
private void updateAccount(AccountModel accountModel, double amount){
            accountModel.setAmount(accountModel.getAmount()-amount);
            accountModel.setDailyTx(accountModel.getDailyTx() + 1);
            accountModel.setDailyTxSum(accountModel.getDailyTxSum() + amount);
            accountRepository.save(accountModel);
}
}
