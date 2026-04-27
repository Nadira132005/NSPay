package com.aciworldwide.aclabs26.services;

import com.aciworldwide.aclabs26.dto.AccountDTO;
import com.aciworldwide.aclabs26.entities.Account;
import com.aciworldwide.aclabs26.repository.AccountRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    private static final Logger logger = LogManager.getLogger(AccountService.class);

    @Autowired
    AccountRepository accountRepository;

    public ResponseEntity<?> getAllAccounts() {
        logger.info("Fetching all accounts from repository");
        return new ResponseEntity<>(accountRepository.getAllAccounts(), new HttpHeaders(), HttpStatus.OK);
    }

    public ResponseEntity<?> getAccountById(long id) {
        logger.info("Fetching account by id {}", id);
        return accountRepository.getAccountById(id)
                .<ResponseEntity<?>>map(account -> new ResponseEntity<>(account, new HttpHeaders(), HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>("Account with id " + id + " is not found", new HttpHeaders(), HttpStatus.NOT_FOUND));
    }

    public ResponseEntity<?> getAccountByCardNumber(String cardNumber) {
        logger.info("Fetching account by card number {}", cardNumber);
        return accountRepository.getAccountByCardNumber(cardNumber)
                .<ResponseEntity<?>>map(account -> new ResponseEntity<>(account, new HttpHeaders(), HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>("Account with card number " + cardNumber + " is not found", new HttpHeaders(), HttpStatus.NOT_FOUND));
    }

    public ResponseEntity<?> addAccount(AccountDTO accountDTO) {
        logger.info("Adding account with card number {}", accountDTO.getCardNumber());
        Account createdAccount = accountRepository.addAccount(toAccount(accountDTO));
        return new ResponseEntity<>(createdAccount, new HttpHeaders(), HttpStatus.CREATED);
    }

    public ResponseEntity<?> updateAccount(long id, AccountDTO accountDTO) {
        logger.info("Updating account with id {}", id);
        return accountRepository.updateAccount(id, toAccount(accountDTO))
                .<ResponseEntity<?>>map(updatedAccount -> new ResponseEntity<>(updatedAccount, new HttpHeaders(), HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>("Account with id " + id + " is not found", new HttpHeaders(), HttpStatus.NOT_FOUND));
    }

    public ResponseEntity<?> deleteAccount(long id) {
        logger.info("Deleting account with id {}", id);
        boolean deleted = accountRepository.deleteAccount(id);
        if (!deleted) {
            return new ResponseEntity<>("Account with id " + id + " is not found", new HttpHeaders(), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private Account toAccount(AccountDTO accountDTO) {
        return Account.builder()
                .cardNumber(accountDTO.getCardNumber())
                .cardHolderName(accountDTO.getCardHolderName())
                .amount(accountDTO.getAmount())
                .dailyTxLimit(accountDTO.getDailyTxLimit())
                .dailyTxSumLimit(accountDTO.getDailyTxSumLimit())
                .dailyTx(accountDTO.getDailyTx())
                .dailyTxSum(accountDTO.getDailyTxSum())
                .build();
    }
}
