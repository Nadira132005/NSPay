package com.nspay.services;

import com.nspay.dto.TransactionDTO;
import com.nspay.entities.AccountModel;
import com.nspay.entities.TransactionModel;
import com.nspay.mappers.TransactionMapper;
import com.nspay.repository.AccountRepository;
import com.nspay.repository.TransactionRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.nspay.validators.TransactionValidator;
import com.nspay.validators.ValidationLogger;
import com.nspay.validators.ValidationResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
@Service
public class TransactionService {

    private static final Logger logger = LogManager.getLogger(TransactionService.class);

    @Autowired private com.nspay.finance.DailyPaymentUsage usage;
    @Autowired
    private TransactionValidator transactionValidator;
    @Autowired
    private ValidationLogger validationLogger;
    @Autowired
    private TransactionMapper transactionMapper;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private AccountRepository accountRepository;

    @Transactional //Processes the transaction and ensures atomicity.If any step fails, all database changes are rolled back.
    public ValidationResults processTransaction(TransactionDTO transactionDTO) {

        logger.info("Processing transaction for card {} and amount {}", transactionDTO.getCardNumber(), transactionDTO.getAmount());
        ValidationResults validationResults = transactionValidator.validate(transactionDTO);
        validationLogger.log(transactionDTO, validationResults);
        if (!validationResults.isValid()) {
            return validationResults;
        }

        TransactionModel transactionModel = transactionMapper.toModel(transactionDTO);
        transactionModel.setTimestamp(java.sql.Timestamp.from(java.time.Instant.now()));
        transactionModel.setTransactionType("CARD_PAYMENT");
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
            transactionRepository.flush();usage.sync(accountModel);
            accountRepository.save(accountModel);
}
public List<TransactionModel> getAllTransactions() {
        return (List<TransactionModel>) transactionRepository.findAll();
    }

public List<TransactionModel> getTransactionsOfAccount(String cardNumber) {
        return transactionRepository.findAllByCardNumber(cardNumber);//created by Spring Data JPA, CrudRepository
    }
}
