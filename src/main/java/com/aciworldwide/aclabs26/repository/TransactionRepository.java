package com.aciworldwide.aclabs26.repository;

import com.aciworldwide.aclabs26.entities.TransactionEntity;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class TransactionRepository {

    private final List<TransactionEntity> transactionsList = new ArrayList<>(Arrays.asList(
            TransactionEntity.builder()
                    .id(1L)
                    .cardNumber("1234000000056789")
                    .amount(2.0)
                    .build(),
            TransactionEntity.builder()
                    .id(2L)
                    .cardNumber("1111222233334444")
                    .amount(5.5)
                    .build()
    ));

    public List<TransactionEntity> getAllTransactions() {
        return transactionsList;
    }

    public Optional<TransactionEntity> getTransactionById(long id) {
        return transactionsList.stream()
                .filter(transaction -> transaction.getId() == id)
                .findFirst();
    }

    public TransactionEntity addTransaction(TransactionEntity transactionEntity) {
        long nextId = transactionsList.stream()
                .mapToLong(TransactionEntity::getId)
                .max()
                .orElse(0L) + 1L;

        TransactionEntity transactionToSave = TransactionEntity.builder()
                .id(transactionEntity.getId() == 0L ? nextId : transactionEntity.getId())
                .cardNumber(transactionEntity.getCardNumber())
                .amount(transactionEntity.getAmount())
                .build();

        transactionsList.add(transactionToSave);
        return transactionToSave;
    }

    public boolean deleteTransaction(long id) {
        return transactionsList.removeIf(transaction -> transaction.getId() == id);
    }
}
