package com.nspay.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name="TRANSACTIONS")
public class TransactionModel {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private long id;

    private String cardNumber;
    private Double amount;
    private Timestamp timestamp;
    private String returnCode;
    private String transactionType;
    private String counterpartyIban;

    public TransactionModel(long id, String cardNumber, Double amount, Timestamp timestamp, String returnCode) {
        this.id = id;
        this.cardNumber = cardNumber;
        this.amount = amount;
        this.timestamp = timestamp;
        this.returnCode = returnCode;
    }

}
