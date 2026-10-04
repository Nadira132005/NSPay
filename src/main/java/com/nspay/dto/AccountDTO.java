package com.nspay.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.nspay.entities.AccountStatus;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDTO {

    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "\\d{16}", message = "Card number must have 16 digits")
    private String cardNumber;

    @NotBlank(message = "Card holder name  required")
    private String cardHolderName;

    private AccountStatus status;

    @Positive(message = "Amount must be greater than zero")
    private double amount;

    @Positive(message = "Daily transaction limit must be greater than zero")
    private int dailyTxLimit;

    @Positive(message = "Daily transaction sum limit must be greater than zero")
    private double dailyTxSumLimit;

    @PositiveOrZero(message = "Daily transaction count cannot be negative")
    private int dailyTx;

    @PositiveOrZero(message = "Daily transaction sum cannot be negative")
    private double dailyTxSum;
}
