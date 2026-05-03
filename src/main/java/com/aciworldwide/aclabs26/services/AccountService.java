package com.aciworldwide.aclabs26.services;

import com.aciworldwide.aclabs26.dto.AccountDTO;
import com.aciworldwide.aclabs26.entities.AccountModel;
import com.aciworldwide.aclabs26.entities.AccountStatus;
import com.aciworldwide.aclabs26.repository.AccountRepository;
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

    public ResponseEntity<?> getAllAccounts() {
        logger.info("Fetching all accounts from repository");
        return new ResponseEntity<>(accountRepository.findAll(), new HttpHeaders(), HttpStatus.OK);
    }

    public ResponseEntity<?> getAccountById(long id) {
        logger.info("Fetching account by id {}", id);
        return accountRepository.findById(id)
                .<ResponseEntity<?>>map(account -> new ResponseEntity<>(account, new HttpHeaders(), HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>("Account with id " + id + " is not found",
                        new HttpHeaders(), HttpStatus.NOT_FOUND));
    }

    public ResponseEntity<?> getAccountByCardNumber(String cardNumber) {
        logger.info("Fetching account by card number {}", cardNumber);
        AccountModel account = accountRepository.findByCardNumber(cardNumber);
        if (account == null) {
            return new ResponseEntity<>("Account with card number " + cardNumber + " is not found",
                    new HttpHeaders(), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(account, new HttpHeaders(), HttpStatus.OK);
    }

    public ResponseEntity<?> addAccount(AccountDTO accountDTO) {
        logger.info("Adding account with card number {}", accountDTO.getCardNumber());
        AccountModel createdAccount = accountRepository.save(toAccount(accountDTO));
        return new ResponseEntity<>(createdAccount, new HttpHeaders(), HttpStatus.CREATED);
    }

    public ResponseEntity<?> updateAccount(long id, AccountDTO accountDTO) {
        logger.info("Updating account with id {}", id);
        Optional<AccountModel> accountOptional = accountRepository.findById(id);
        if (!accountOptional.isPresent()) {
            return new ResponseEntity<>("Account with id " + id + " is not found",
                    new HttpHeaders(), HttpStatus.NOT_FOUND);
        }

        AccountModel account = accountOptional.get();
        apply(account, accountDTO);
        return new ResponseEntity<>(accountRepository.save(account), new HttpHeaders(), HttpStatus.OK);
    }

    public ResponseEntity<?> deleteAccount(long id) {
        logger.info("Deleting account with id {}", id);
        if (!accountRepository.existsById(id)) {
            return new ResponseEntity<>("Account with id " + id + " is not found",
                    new HttpHeaders(), HttpStatus.NOT_FOUND);
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
        account.setAmount(accountDTO.getAmount());
        account.setDailyTxLimit(accountDTO.getDailyTxLimit());
        account.setDailyTxSumLimit(accountDTO.getDailyTxSumLimit());
        account.setDailyTx(accountDTO.getDailyTx());
        account.setDailyTxSum(accountDTO.getDailyTxSum());
    }
}
