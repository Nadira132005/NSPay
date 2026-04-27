package com.aciworldwide.aclabs26.repository;

import com.aciworldwide.aclabs26.entities.Account;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

@Repository
public class AccountRepository {
    private final List<Account> accountsList = Arrays.asList(
            new Account(1L, "1234000000056789", "John Black", 15.00, 1000000, 1000000000.00, 0, 0.00),
            new Account(2L, "1111222233334444", "Lara Dean", 89.00, 1000000, 1000000000.00, 0, 0.00),
            new Account(3L, "4321000000056789", "Alice Bond", 1143.00, 1000000, 1000000000.00, 0, 0.00)
    );

    public List<Account> getFullAccounts() {
        return accountsList;
    }

    public Account getAccountById(long id) {
        return accountsList.stream().filter(account -> account.getId() == id).findFirst().orElse(null);
    }

    public void addAccount(Account account) {
        accountsList.add(account);
    }

    public void removeAccount(Account account) {
        accountsList.remove(account);
    }
}
