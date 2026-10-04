package com.aciworldwide.aclabs26.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@Entity
@NoArgsConstructor
@Table(name="ACCOUNTS")
public class AccountModel {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private long id;

    @Column(unique = true)
    private String cardNumber;

    @Column(unique = true, length = 34)
    private String iban;

    private String cardHolderName;
    @Enumerated(EnumType.STRING)
    private AccountStatus status = AccountStatus.ACTIVE;
    private double amount;
    private int dailyTxLimit;
    private double dailyTxSumLimit;
    private int dailyTx;
    private double dailyTxSum;


    public boolean dailyTxLimitExceeded() {
        return dailyTx > dailyTxLimit;
    }
}
