package com.nspay.config;

import com.nspay.entities.*;
import com.nspay.finance.*;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/** Response descriptions for composed views; runtime services assemble the same fields. */
public final class ApiViews {
    private ApiViews() {}
    @Data @Schema(name="FinancialOverview") public static class Overview {
        private UserModel user;private AccountModel account;private String currency;private DailyPaymentUsage.Usage dailyUsage;
        private List<SavingsPocket> pockets;private List<ScheduledTransfer> scheduled;private List<TradeOrder> trades;
        private List<CreditApplication> credits;private List<CreditDocument> documents;private CustomerProfile profile;
        private boolean onboardingAvailable;private boolean creditEligible;
        private List<TransactionModel> transactions;
    }
    @Data @Schema(name="InvestmentPortfolio") public static class Portfolio {
        @Schema(description="Available investment cash in EUR") private BigDecimal cash;
        private List<TradeOrder> orders;private List<MarketDataService.Instrument> catalog;
        private List<Holding> holdings;private boolean streaming;
        @Schema(description="Cash plus current holdings value; null when any holding is unpriced") private BigDecimal totalValue;
        private BigDecimal holdingsValue;private BigDecimal costBasis;private BigDecimal unrealizedGain;private BigDecimal unrealizedGainPercent;private BigDecimal realizedGain;private BigDecimal totalGain;
        private String currency;private String provider;private long orderCount;private java.time.Instant priceAt;private java.time.Instant valuedAt;
        @Schema(allowableValues={"COMPLETE","INCOMPLETE"}) private String valuationStatus;private String valuationError;
        @Schema(description="Provider configured; CuckooTrade requires no API key") private boolean configured;
    }
    @Data @Schema(name="InvestmentHolding") public static class Holding {
        private String symbol;private String provider;private BigDecimal quantity;private BigDecimal costBasis;private BigDecimal averageCost;
        private BigDecimal accountingBasis;private BigDecimal price;private BigDecimal eurPrice;private BigDecimal marketValue;private BigDecimal unrealizedGain;private BigDecimal unrealizedGainPercent;private BigDecimal allocationPercent;
        private java.time.Instant priceAt;private String valuationStatus;
    }
    @Data @Schema(name="CustomerSecurityState") public static class SecurityState {
        private AccountRestriction restriction;private List<SecurityReport> reports;private List<StatusChange> history;
        private List<SecurityActivity> activity;
    }
    @Data @Schema(name="PublicSecurityActivity") public static class SecurityActivity {private long id;private String username;private String status;private java.time.Instant createdAt;private Integer misalignmentScore;private String publicNote;@Schema(description="Omitted unless the viewer is the affected customer") private String privateNote;@Schema(description="Omitted unless the viewer is the affected customer") private String reason;@Schema(description="Omitted unless the viewer is the affected customer") private Long transactionId;}
    @Data @Schema(name="AccountLimitQueue") public static class AccountLimitQueue {private List<AccountLimitEntry> items;private long total;private int page;private int size;}
    @Data @Schema(name="AccountLimitEntry") public static class AccountLimitEntry {private long id;private Long userId;private String name;private String username;private String iban;private String card;private AccountStatus status;private BigDecimal balance;private DailyPaymentUsage.Usage usage;}
    @Data @Schema(name="AdministratorPaymentQueue") public static class PaymentQueue {private List<PaymentSignal> items;private long total;private int page;private int size;private boolean indexing;}
    @Data @Schema(name="AdministratorCreditQueue") public static class CreditQueue {private List<CreditQueueEntry> items;private long total;private int page;private int size;}
    @Data @Schema(name="CreditQueueEntry") public static class CreditQueueEntry {private CreditApplication application;private String username;}
    @Data @Schema(name="AdministratorDirectoryEntry") public static class DirectoryEntry {
        private long id;private String username;private String name;private com.nspay.enums.UserRole role;
        private AccountStatus status;private BigDecimal balance;private String iban;
        private String onboardingStatus;
        @Schema(example="•••• •••• •••• 4444",description="Masked card number: only the last four digits") private String card;
        private AccountRestriction restriction;
    }
    @Data @Schema(name="ReviewQueue") public static class ReviewQueue {
        private List<RiskAssessment> risks;private List<CreditApplication> credits;
    }
    @Data @Schema(name="OnboardingState") public static class Onboarding {
        private CustomerProfile profile;@Schema(allowableValues={"DETAILS","IDENTITY"}) private String step;
    }
    @Data @Schema(name="OnboardingReviewEntry") public static class OnboardingReview {
        private long userId;private String username;private CustomerProfile profile;private List<CreditDocument> documents;
    }
    @Data @Schema(name="StockPriceHistory") public static class TimeSeries {
        private Meta meta;private List<PriceBar> values;private String status;private String provider;private boolean synthetic;
    }
    @Data @Schema(name="StockHistoryMetadata") public static class Meta {
        private String symbol;private String interval;private String currency;private String exchange;private String exchange_timezone;
    }
    @Data @Schema(name="StockPriceBar") public static class PriceBar {
        @Schema(description="UTC bar date/time; YYYY-MM-DD for daily bars",example="2026-10-02 14:30:00") private String datetime;
        private String open;private String high;private String low;private String close;private String volume;
    }
}
