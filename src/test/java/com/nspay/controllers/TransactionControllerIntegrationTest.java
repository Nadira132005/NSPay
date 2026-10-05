package com.nspay.controllers;

import com.nspay.entities.AccountModel;
import com.nspay.entities.AccountStatus;
import com.nspay.entities.TransactionModel;
import com.nspay.repository.AccountRepository;
import com.nspay.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        accountRepository.save(createAccount("1234567812345678", "Test User", 100.0, 10, 1000.0));
    }

    @Test
    void shouldProcessSuccessfulTransaction() throws Exception {
        mockMvc.perform(post("/transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cardNumber\":\"1234567812345678\",\"amount\":25.0,\"timestamp\":\"2026-05-03T10:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.code", is("00")));

        AccountModel account = accountRepository.findByCardNumber("1234567812345678");
        org.assertj.core.api.Assertions.assertThat(account).isNotNull();
        org.assertj.core.api.Assertions.assertThat(account.getAmount()).isEqualTo(75.0);
        org.assertj.core.api.Assertions.assertThat(transactionRepository.findAll()).hasSize(1);
    }

    @Test
    void shouldReturnAllTransactions() throws Exception {
        mockMvc.perform(post("/transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cardNumber\":\"1234567812345678\",\"amount\":25.0,\"timestamp\":\"2026-05-03T10:00:00\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].cardNumber", is("1234567812345678")));
    }

    @Test
    void shouldReturnTransactionsByCardNumber() throws Exception {
        mockMvc.perform(post("/transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cardNumber\":\"1234567812345678\",\"amount\":25.0,\"timestamp\":\"2026-05-03T10:00:00\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/transactions/1234567812345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].returnCode", is("00")));
    }

    @Test
    void shouldReturnInsufficientFundsAndPersistTransaction() throws Exception {
        mockMvc.perform(post("/transaction")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cardNumber\":\"1234567812345678\",\"amount\":250.0,\"timestamp\":\"2026-05-03T10:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.businessLogicViolation", is(true)))
                .andExpect(jsonPath("$.code", is("51")));

        AccountModel account = accountRepository.findByCardNumber("1234567812345678");
        org.assertj.core.api.Assertions.assertThat(account).isNotNull();
        org.assertj.core.api.Assertions.assertThat(account.getAmount()).isEqualTo(100.0);

        TransactionModel saved = transactionRepository.findAll().iterator().next();
        org.assertj.core.api.Assertions.assertThat(saved.getReturnCode()).isEqualTo("51");
    }

    @Test
    void shouldAllowPaymentsAboveConsumedAdvisoryLimits() throws Exception {
        AccountModel account = accountRepository.findByCardNumber("1234567812345678");
        account.setDailyTxLimit(1);account.setDailyTxSumLimit(10);account.setDailyTx(1);account.setDailyTxSum(10);account.setAmount(90);
        accountRepository.save(account);
        TransactionModel prior=new TransactionModel();prior.setCardNumber(account.getCardNumber());prior.setAmount(10.0);prior.setTimestamp(java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(60)));prior.setReturnCode("00");prior.setTransactionType("CARD_PAYMENT");transactionRepository.save(prior);
        mockMvc.perform(post("/transaction").contentType(MediaType.APPLICATION_JSON)
            .content("{\"cardNumber\":\"1234567812345678\",\"amount\":10.0,\"timestamp\":\"2026-05-03T10:00:00\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code",is("00"))).andExpect(jsonPath("$.businessLogicViolation",is(false)));
        AccountModel result=accountRepository.findByCardNumber(account.getCardNumber());
        org.assertj.core.api.Assertions.assertThat(result.getAmount()).isEqualTo(80);
        org.assertj.core.api.Assertions.assertThat(result.getDailyTx()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(result.getDailyTxSum()).isEqualTo(20);
        org.assertj.core.api.Assertions.assertThat(result.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void shouldRejectBlockedAccount() throws Exception {
        AccountModel account = accountRepository.findByCardNumber("1234567812345678");
        account.setStatus(AccountStatus.BLOCKED);
        accountRepository.save(account);

        mockMvc.perform(post("/transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cardNumber\":\"1234567812345678\",\"amount\":10.0,\"timestamp\":\"2026-05-03T10:00:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("13")))
                .andExpect(jsonPath("$.valid", is(false)));
    }

    @Test
    void shouldRejectInvalidTransactionPayload() throws Exception {
        mockMvc.perform(post("/transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cardNumber\":\"\",\"amount\":-10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("Card number is required")));
    }

    private AccountModel createAccount(String cardNumber, String cardHolderName, double amount, int dailyTxLimit, double dailyTxSumLimit) {
        AccountModel account = new AccountModel();
        account.setCardNumber(cardNumber);
        account.setCardHolderName(cardHolderName);
        account.setAmount(amount);
        account.setDailyTxLimit(dailyTxLimit);
        account.setDailyTxSumLimit(dailyTxSumLimit);
        account.setDailyTx(0);
        account.setDailyTxSum(0.0);
        account.setStatus(AccountStatus.ACTIVE);
        return account;
    }
}
