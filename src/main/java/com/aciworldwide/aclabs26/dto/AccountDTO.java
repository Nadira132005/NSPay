package com.aciworldwide.aclabs26.dto;

import com.aciworldwide.aclabs26.entities.AccountModel;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AccountDTO {
    private String cardNumber;
    private Double amount;
    private String cardHolderName;
    private Integer dailyTxLimit;
    private Double dailyTxSumLimit;
    private Integer dailyTx;
    private Double dailyTxSum;
    private String status;

    public AccountDTO(AccountModel account) {
        cardNumber = account.getCardNumber();
        amount = account.getAmount();
        dailyTxLimit = account.getDailyTxLimit();
        dailyTxSumLimit = account.getDailyTxSumLimit();
        dailyTx = account.getDailyTx();
        dailyTxSum = account.getDailyTxSum();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountDTO that = (AccountDTO) o;
        return Objects.equals(cardNumber, that.cardNumber) && Objects.equals(amount, that.amount) && Objects.equals(cardHolderName, that.cardHolderName) && Objects.equals(dailyTxLimit, that.dailyTxLimit) && Objects.equals(dailyTxSumLimit, that.dailyTxSumLimit) && Objects.equals(dailyTx, that.dailyTx) && Objects.equals(dailyTxSum, that.dailyTxSum) && Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardNumber, amount, cardHolderName, dailyTxLimit, dailyTxSumLimit, dailyTx, dailyTxSum, status);
    }
}
