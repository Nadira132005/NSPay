package com.aciworldwide.aclabs26.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDTO {

    private String cardNumber;
    private String cardHolderName;
    private double amount;
    private int dailyTxLimit;
    private double dailyTxSumLimit;
    private int dailyTx;
    private double dailyTxSum;
}
