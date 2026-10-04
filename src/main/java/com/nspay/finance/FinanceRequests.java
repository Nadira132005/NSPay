package com.nspay.finance;

import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class FinanceRequests {
    private FinanceRequests() { }
    @io.swagger.v3.oas.annotations.media.Schema(name="FinancePocket")
    @Data public static class Pocket {
        @NotBlank @Size(max=60) private String name;
        @NotNull @DecimalMin("0.00") @Digits(integer=10, fraction=2) private BigDecimal initialAmount;
        @NotNull @DecimalMin("0.01") @Digits(integer=10, fraction=2) private BigDecimal targetAmount;
    }
    @io.swagger.v3.oas.annotations.media.Schema(name="FinanceMoney")
    @Data public static class Money {
        @NotNull @DecimalMin("0.01") @Digits(integer=10, fraction=2) private BigDecimal amount;
    }
    @io.swagger.v3.oas.annotations.media.Schema(name="FinancePayment")
    @Data public static class Payment {
        @NotBlank @Pattern(regexp="[A-Za-z0-9 ]{15,34}") private String recipientIban;
        @NotNull @DecimalMin("0.01") @Digits(integer=10, fraction=2) private BigDecimal amount;
        @Future @io.swagger.v3.oas.annotations.media.Schema(example="2026-12-01T10:00:00Z") private Instant scheduledAt;
    }
    @io.swagger.v3.oas.annotations.media.Schema(name="FinanceTrade")
    @Data public static class Trade {
        @NotBlank private String symbol;
        @Pattern(regexp="BUY|SELL") @NotNull private String side;
        @NotNull @DecimalMin("0.000001") @Digits(integer=6, fraction=6) private BigDecimal quantity;
    }
    @io.swagger.v3.oas.annotations.media.Schema(name="FinanceCredit")
    @Data public static class Credit {
        @NotNull @DecimalMin("100.00") @DecimalMax("100000.00") @Digits(integer=6, fraction=2) private BigDecimal amount;
        @NotNull @DecimalMin("1.00") @Digits(integer=7, fraction=2) private BigDecimal monthlyIncome;
        @Min(3) @Max(60) private int termMonths;
        @NotBlank @Size(max=250) private String purpose;
    }
    @io.swagger.v3.oas.annotations.media.Schema(name="FinanceProfile")
    @Data public static class Profile {
        @NotBlank @Email @Size(max=254) private String email;
        @NotBlank @Size(max=30) private String phone;
        @NotBlank @Size(max=60) private String country;
        @NotNull @Past private LocalDate dateOfBirth;
        @NotBlank @Size(max=100) private String occupation;
    }
    @io.swagger.v3.oas.annotations.media.Schema(name="FinanceReview")
    @Data public static class Review {
        @NotBlank @Size(min=5, max=250) private String note;
        @NotNull @Pattern(regexp="APPROVED|DECLINED|REVIEWED") private String decision;
    }
}
