package com.aciworldwide.aclabs26.services;

import com.aciworldwide.aclabs26.dto.TransactionDTO;
import com.aciworldwide.aclabs26.entities.AccountModel;
import com.aciworldwide.aclabs26.entities.TransactionModel;
import com.aciworldwide.aclabs26.repository.AccountRepository;
import com.aciworldwide.aclabs26.repository.TransactionRepository;
import com.aciworldwide.aclabs26.validators.TransactionValidator;
import com.aciworldwide.aclabs26.validators.ValidationResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

@Service
public class TransactionService {

    @Autowired
    private TransactionValidator transactionValidator;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private AccountRepository accountRepository;

    @Transactional
    public ValidationResults processTransaction(TransactionDTO transactionDTO) {
        ValidationResults validationResults = transactionValidator.validate(transactionDTO);

        if (!validationResults.isValid()) {
            return validationResults;
        }

        TransactionModel transactionModel = new TransactionModel(transactionDTO);
        transactionModel.setReturnCode(validationResults.getCode());
        transactionRepository.save(transactionModel);

        if(validationResults.isBusinessLogicViolation()){
            return validationResults;
        }

        AccountModel accountModel = accountRepository.findAccountByCardNumber(transactionDTO.getCardNumber());
        updateAccount(accountModel, transactionDTO.getAmount());

        return validationResults;

    }

    private void updateAccount(AccountModel accountModel, double amount){
            accountModel.setAmount(accountModel.getAmount()-amount);
            accountModel.setDailyTx(accountModel.getDailyTx() + 1);
            accountModel.setDailyTxSum(accountModel.getDailyTxSum() + amount);
    }

    public List<TransactionModel> getAllTransactions() {
        return (List<TransactionModel>) transactionRepository.findAll();
    }


    public List<TransactionModel> getTransactionsOfAccount(String cardNumber) {
        return transactionRepository.findAllByCardNumber(cardNumber);
    }
}
