package com.aciworldwide.aclabs26.services;

import com.aciworldwide.aclabs26.entities.Account;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

public class AccountService {

    private static final List<Account> accountsList = Arrays.asList(
            new Account(1L, "1234000000056789", "John Black", 15.00, 1000000, 1000000000.00, 0, 0.00),
            new Account(2L, "1111222233334444", "Lara Dean", 89.00, 1000000, 1000000000.00, 0, 0.00),
            new Account(3L, "4321000000056789", "Alice Bond", 1143.00, 1000000, 1000000000.00, 0, 0.00)
    );

    public ResponseEntity<?> getAllAccounts() {
        return new ResponseEntity<>(accountsList, new HttpHeaders(), HttpStatus.OK);
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
