package com.aciworldwide.aclabs26.dto;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class TransactionDTO {
    private String cardNumber;
    private Double amount;
    private Timestamp timestamp;
}
