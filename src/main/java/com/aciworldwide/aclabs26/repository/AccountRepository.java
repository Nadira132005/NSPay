package com.aciworldwide.aclabs26.repository;

import com.aciworldwide.aclabs26.entities.Account;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class AccountRepository {
    private final List<Account> accountsList = new ArrayList<>(Arrays.asList(
            Account.builder()
                    .id(1L)
                    .cardNumber("1234000000056789")
                    .cardHolderName("John Black")
                    .amount(15.00)
                    .dailyTxLimit(1000000)
                    .dailyTxSumLimit(1000000000.00)
                    .dailyTx(0)
                    .dailyTxSum(0.00)
                    .build(),
            Account.builder()
                    .id(2L)
                    .cardNumber("1111222233334444")
                    .cardHolderName("Lara Dean")
                    .amount(89.00)
                    .dailyTxLimit(1000000)
                    .dailyTxSumLimit(1000000000.00)
                    .dailyTx(0)
                    .dailyTxSum(0.00)
                    .build(),
            Account.builder()
                    .id(3L)
                    .cardNumber("4321000000056789")
                    .cardHolderName("Alice Bond")
                    .amount(1143.00)
                    .dailyTxLimit(1000000)
                    .dailyTxSumLimit(1000000000.00)
                    .dailyTx(0)
                    .dailyTxSum(0.00)
                    .build()
    ));

    public List<Account> getAllAccounts() {
        return accountsList;
    }

    public Optional<Account> getAccountById(long id) {
        return accountsList.stream()
                .filter(account -> account.getId() == id)
                .findFirst();
    }

    public Optional<Account> getAccountByCardNumber(String cardNumber) {
        return accountsList.stream()
                .filter(account -> account.getCardNumber().equals(cardNumber))
                .findFirst();
    }

    public Account addAccount(Account account) {
        long nextId = accountsList.stream()
                .mapToLong(Account::getId)
                .max()
                .orElse(0L) + 1L;
        Account accountToSave = Account.builder()
                .id(account.getId() == 0L ? nextId : account.getId())
                .cardNumber(account.getCardNumber())
                .cardHolderName(account.getCardHolderName())
                .amount(account.getAmount())
                .dailyTxLimit(account.getDailyTxLimit())
                .dailyTxSumLimit(account.getDailyTxSumLimit())
                .dailyTx(account.getDailyTx())
                .dailyTxSum(account.getDailyTxSum())
                .build();
        accountsList.add(accountToSave);
        return accountToSave;
    }

    public Optional<Account> updateAccount(long id, Account account) {
        for (int i = 0; i < accountsList.size(); i++) {
            if (accountsList.get(i).getId() == id) {
                Account updatedAccount = Account.builder()
                        .id(id)
                        .cardNumber(account.getCardNumber())
                        .cardHolderName(account.getCardHolderName())
                        .amount(account.getAmount())
                        .dailyTxLimit(account.getDailyTxLimit())
                        .dailyTxSumLimit(account.getDailyTxSumLimit())
                        .dailyTx(account.getDailyTx())
                        .dailyTxSum(account.getDailyTxSum())
                        .build();
                accountsList.set(i, updatedAccount);
                return Optional.of(updatedAccount);
            }
        }
        return Optional.empty();
    }

    public boolean deleteAccount(long id) {
        return accountsList.removeIf(account -> account.getId() == id);
    }
}
