package com.aciworldwide.aclabs26.services;

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

    public ResponseEntity<?> getAccount() {
        return null;
    }

    public ResponseEntity<?> addAccount() {
        return null;
    }

    public ResponseEntity<?> updateAccount() {
        return null;
    }

    public ResponseEntity<?> deleteAccount() { return null; }
}
