package com.aciworldwide.aclabs26.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {
    @Schema(description = "Card number of the account")
    @NotBlank(message = "Card number is required")
    private String cardNumber;

    @Schema(description = "Transaction amount")
    @Positive(message = "Amount must be greater than zero")
    private Double amount;
    private Timestamp timestamp;
}
