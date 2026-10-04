package com.nspay.repository;

import com.nspay.entities.TransactionModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void shouldSaveTransactionAndReturnGeneratedId() {
        TransactionModel transaction = new TransactionModel();
        transaction.setCardNumber("1234567812345678");
        transaction.setAmount(25.0);
        transaction.setReturnCode("00");

        TransactionModel saved = transactionRepository.save(transaction);

        assertThat(saved.getId()).isGreaterThan(0L);
        assertThat(saved.getCardNumber()).isEqualTo("1234567812345678");
        assertThat(saved.getReturnCode()).isEqualTo("00");
    }

    @Test
    void shouldFindTransactionById() {
        TransactionModel transaction = new TransactionModel();
        transaction.setCardNumber("9999888877776666");
        transaction.setAmount(12.5);
        transaction.setReturnCode("51");

        TransactionModel saved = transactionRepository.save(transaction);

        assertThat(transactionRepository.findById(saved.getId()))
                .isPresent()
                .get()
                .extracting(TransactionModel::getReturnCode)
                .isEqualTo("51");
    }

    @Test
    void shouldReturnAllSavedTransactions() {
        TransactionModel first = new TransactionModel();
        first.setCardNumber("1010101010101010");
        first.setAmount(5.0);
        first.setReturnCode("00");

        TransactionModel second = new TransactionModel();
        second.setCardNumber("2020202020202020");
        second.setAmount(7.5);
        second.setReturnCode("51");

        transactionRepository.save(first);
        transactionRepository.save(second);

        assertThat(transactionRepository.findAll())
                .hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldDeleteTransactionById() {
        TransactionModel transaction = new TransactionModel();
        transaction.setCardNumber("3030303030303030");
        transaction.setAmount(11.0);
        transaction.setReturnCode("00");

        TransactionModel saved = transactionRepository.save(transaction);
        transactionRepository.deleteById(saved.getId());

        assertThat(transactionRepository.findById(saved.getId())).isEmpty();
    }
}
