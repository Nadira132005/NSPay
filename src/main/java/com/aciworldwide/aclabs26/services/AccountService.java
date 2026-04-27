package com.aciworldwide.aclabs26.services;

import com.aciworldwide.aclabs26.entities.Account;
import com.aciworldwide.aclabs26.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    public ResponseEntity<?> getAllAccounts() {
        return new ResponseEntity<>(accountRepository.getFullAccounts(), new HttpHeaders(), HttpStatus.OK);
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
