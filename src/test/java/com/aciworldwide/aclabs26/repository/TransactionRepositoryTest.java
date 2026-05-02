package com.aciworldwide.aclabs26.repository;

import com.aciworldwide.aclabs26.entities.TransactionModel;
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
}
