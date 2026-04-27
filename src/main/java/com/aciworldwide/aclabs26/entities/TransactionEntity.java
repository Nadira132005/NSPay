package com.aciworldwide.aclabs26.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEntity {

    private long id;
    private String cardNumber;
    private double amount;
}
