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
        private UserModel user;private AccountModel account;private String currency;
        private List<SavingsPocket> pockets;private List<ScheduledTransfer> scheduled;private List<TradeOrder> trades;
        private List<CreditApplication> credits;private List<CreditDocument> documents;private CustomerProfile profile;
        private List<TransactionModel> transactions;
    }
    @Data @Schema(name="InvestmentPortfolio") public static class Portfolio {
        @Schema(description="Available investment cash in EUR") private BigDecimal cash;
        private List<TradeOrder> orders;private List<MarketDataService.Instrument> catalog;
        @Schema(description="A server market-data API key is configured; provider access can still fail") private boolean configured;
    }
    @Data @Schema(name="CustomerSecurityState") public static class SecurityState {
        private AccountRestriction restriction;private List<SecurityReport> reports;private List<StatusChange> history;
    }
    @Data @Schema(name="AdministratorDirectoryEntry") public static class DirectoryEntry {
        private long id;private String username;private String name;private com.nspay.enums.UserRole role;
        private AccountStatus status;private BigDecimal balance;private String iban;
        @Schema(example="•••• •••• •••• 4444",description="Masked card number: only the last four digits") private String card;
        private AccountRestriction restriction;
    }
    @Data @Schema(name="ReviewQueue") public static class ReviewQueue {
        private List<RiskAssessment> risks;private List<CreditApplication> credits;
    }
    @Data @Schema(name="StockPriceHistory") public static class TimeSeries {
        private Meta meta;private List<PriceBar> values;private String status;
    }
    @Data @Schema(name="StockHistoryMetadata") public static class Meta {
        private String symbol;private String interval;private String currency;private String exchange;private String exchange_timezone;
    }
    @Data @Schema(name="StockPriceBar") public static class PriceBar {
        @Schema(description="UTC bar date/time; YYYY-MM-DD for daily bars",example="2026-10-02 14:30:00") private String datetime;
        private String open;private String high;private String low;private String close;private String volume;
    }
}
