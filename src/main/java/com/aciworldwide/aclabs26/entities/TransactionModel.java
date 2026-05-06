package com.aciworldwide.aclabs26.entities;

import com.aciworldwide.aclabs26.dto.TransactionDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "TRANSACTIONS")
public class TransactionModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String cardNumber;
    private Double amount;
    private Timestamp timestamp;
    private String returnCode;

    public TransactionModel(TransactionDTO transactionDTO) {
        this.cardNumber = transactionDTO.getCardNumber();
        this.amount = transactionDTO.getAmount();
        this.timestamp = transactionDTO.getTimestamp();
    }

}
