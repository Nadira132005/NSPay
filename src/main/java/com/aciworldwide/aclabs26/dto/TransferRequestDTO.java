package com.aciworldwide.aclabs26.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Data
public class TransferRequestDTO {
    @NotBlank
    private String recipientIban;

    @NotNull
    @Positive
    private Double amount;
}
