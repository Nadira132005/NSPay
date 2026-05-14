package com.aciworldwide.aclabs26.services;

import com.aciworldwide.aclabs26.dto.AccountDTO;
import com.aciworldwide.aclabs26.entities.AccountModel;
import com.aciworldwide.aclabs26.entities.AccountStatus;
import com.aciworldwide.aclabs26.exceptions.ResourceNotFoundException;
import com.aciworldwide.aclabs26.repository.AccountRepository;
import com.aciworldwide.aclabs26.validators.LuhnValidator;
import com.aciworldwide.aclabs26.exceptions.ValidationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AccountService {

    private static final Logger logger = LogManager.getLogger(AccountService.class);

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private LuhnValidator luhnValidator;

    public ResponseEntity<?> getAllAccounts() {
        logger.info("Fetching all accounts from repository");
        return new ResponseEntity<>(accountRepository.findAll(), new HttpHeaders(), HttpStatus.OK);
    }

    public ResponseEntity<?> getAccountById(long id) {
        logger.info("Fetching account by id {}", id);
        return accountRepository.findById(id)
                .<ResponseEntity<?>>map(account -> new ResponseEntity<>(account, new HttpHeaders(), HttpStatus.OK))
                .orElseThrow(() -> new ResourceNotFoundException("Account with id " + id + " is not found"));
    }

    public ResponseEntity<?> getAccountByCardNumber(String cardNumber) {
        logger.info("Fetching account by card number {}", cardNumber);
        AccountModel account = accountRepository.findByCardNumber(cardNumber);
        if (account == null) {
            throw new ResourceNotFoundException("Account with card number " + cardNumber + " is not found");
        }
        return new ResponseEntity<>(account, new HttpHeaders(), HttpStatus.OK);
    }

    public ResponseEntity<?> addAccount(AccountDTO accountDTO) {
        logger.info("Adding account with card number {}", accountDTO.getCardNumber());
        if (!luhnValidator.isValid(accountDTO.getCardNumber())) {
            throw new ValidationException("Card number failed Luhn validation");
        }
        AccountModel createdAccount = accountRepository.save(toAccount(accountDTO));
        return new ResponseEntity<>(createdAccount, new HttpHeaders(), HttpStatus.CREATED);
    }

    public ResponseEntity<?> updateAccount(long id, AccountDTO accountDTO) {
        logger.info("Updating account with id {}", id);
        Optional<AccountModel> accountOptional = accountRepository.findById(id);
        if (!accountOptional.isPresent()) {
            throw new ResourceNotFoundException("Account with id " + id + " is not found");
        }

        AccountModel account = accountOptional.get();
        apply(account, accountDTO);
        return new ResponseEntity<>(accountRepository.save(account), new HttpHeaders(), HttpStatus.OK);
    }

    public ResponseEntity<?> suspendAccount(long id) {
        logger.info("Suspending account with id {}", id);
        Optional<AccountModel> accountOptional = accountRepository.findById(id);
        if (!accountOptional.isPresent()) {
            throw new ResourceNotFoundException("Account with id " + id + " is not found");
        }

        AccountModel account = accountOptional.get();
        account.setStatus(AccountStatus.SUSPENDED);
        accountRepository.save(account);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    public ResponseEntity<?> deleteAccount(long id) {
        logger.info("Deleting account with id {}", id);
        if (!accountRepository.existsById(id)) {
            throw new ResourceNotFoundException("Account with id " + id + " is not found");
        }
        accountRepository.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private AccountModel toAccount(AccountDTO accountDTO) {
        AccountModel account = new AccountModel();
        apply(account, accountDTO);
        account.setStatus(AccountStatus.ACTIVE);
        return account;
    }

    private void apply(AccountModel account, AccountDTO accountDTO) {
        account.setCardNumber(accountDTO.getCardNumber());
        account.setCardHolderName(accountDTO.getCardHolderName());
        if (accountDTO.getStatus() != null) {
            account.setStatus(accountDTO.getStatus());
        }
        account.setAmount(accountDTO.getAmount());
        account.setDailyTxLimit(accountDTO.getDailyTxLimit());
        account.setDailyTxSumLimit(accountDTO.getDailyTxSumLimit());
        account.setDailyTx(accountDTO.getDailyTx());
        account.setDailyTxSum(accountDTO.getDailyTxSum());
    }
}
