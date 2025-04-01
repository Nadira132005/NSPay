package com.aciworldwide.aclabs22.entities;

public class Account {

    private long id;
    private String cardNumber;
    private String cardHolderName;
    private double amount;
    private int dailyTxLimit;
    private double dailyTxSumLimit;
    private int dailyTx;
    private double dailyTxSum;


    public Account(long id, String cardNumber, String cardHolderName, double amount, int dailyTxLimit, double dailyTxSumLimit, int dailyTx, double dailyTxSum) {
        this.id = id;
        this.cardNumber = cardNumber;
        this.cardHolderName = cardHolderName;
        this.amount = amount;
        this.dailyTxLimit = dailyTxLimit;
        this.dailyTxSumLimit = dailyTxSumLimit;
        this.dailyTx = dailyTx;
        this.dailyTxSum = dailyTxSum;
    }
}
