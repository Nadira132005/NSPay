package com.aciworldwide.aclabs26.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {
    @NotBlank(message = "Card number is required")
    private String cardNumber;
    @Positive(message = "Amount must be greater than zero")
    private double amount;
}
