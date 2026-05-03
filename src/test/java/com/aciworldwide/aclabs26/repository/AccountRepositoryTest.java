package com.aciworldwide.aclabs26.repository;

import com.aciworldwide.aclabs26.entities.AccountModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;
//we simulate the connection with the real database using in-memory database

@DataJpaTest //repository/database tests
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void shouldSaveAndFindAccountByCardNumber() {
        AccountModel account = new AccountModel();
        account.setCardNumber("1234567812345678");
        account.setCardHolderName("Test User");
        account.setAmount(100.0);
        account.setDailyTxLimit(10);
        account.setDailyTxSumLimit(1000.0);
        account.setDailyTx(0);
        account.setDailyTxSum(0.0);

        AccountModel saved = accountRepository.save(account);
        AccountModel found = accountRepository.findByCardNumber("1234567812345678");

        assertThat(saved.getId()).isGreaterThan(0L);
        assertThat(found).isNotNull();
        assertThat(found.getCardHolderName()).isEqualTo("Test User");
        assertThat(found.getAmount()).isEqualTo(100.0);
    }

    @Test
    void shouldReturnAllSavedAccounts() {
        AccountModel first = new AccountModel();
        first.setCardNumber("1111222233334444");
        first.setCardHolderName("First User");
        first.setAmount(50.0);
        first.setDailyTxLimit(5);
        first.setDailyTxSumLimit(500.0);

        AccountModel second = new AccountModel();
        second.setCardNumber("5555666677778888");
        second.setCardHolderName("Second User");
        second.setAmount(75.0);
        second.setDailyTxLimit(7);
        second.setDailyTxSumLimit(700.0);

        accountRepository.save(first);
        accountRepository.save(second);

        assertThat(accountRepository.findAll())
                .hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldUpdateExistingAccountWhenSavedWithSameId() {
        AccountModel account = new AccountModel();
        account.setCardNumber("2222333344445555");
        account.setCardHolderName("Original User");
        account.setAmount(10.0);
        account.setDailyTxLimit(2);
        account.setDailyTxSumLimit(200.0);
        account.setDailyTx(0);
        account.setDailyTxSum(0.0);

        AccountModel saved = accountRepository.save(account);

        saved.setCardHolderName("Updated User");
        saved.setAmount(25.0);
        AccountModel updated = accountRepository.save(saved);

        AccountModel found = accountRepository.findById(updated.getId()).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getCardHolderName()).isEqualTo("Updated User");
        assertThat(found.getAmount()).isEqualTo(25.0);
    }

    @Test
    void shouldDeleteAccountById() {
        AccountModel account = new AccountModel();
        account.setCardNumber("3333444455556666");
        account.setCardHolderName("Delete User");
        account.setAmount(40.0);
        account.setDailyTxLimit(4);
        account.setDailyTxSumLimit(400.0);
        account.setDailyTx(0);
        account.setDailyTxSum(0.0);

        AccountModel saved = accountRepository.save(account);
        accountRepository.deleteById(saved.getId());

        assertThat(accountRepository.findById(saved.getId())).isEmpty();
    }
}
