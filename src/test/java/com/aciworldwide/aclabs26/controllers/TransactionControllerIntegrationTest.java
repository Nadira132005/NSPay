package com.aciworldwide.aclabs26.controllers;

import com.aciworldwide.aclabs26.entities.AccountModel;
import com.aciworldwide.aclabs26.entities.AccountStatus;
import com.aciworldwide.aclabs26.entities.TransactionModel;
import com.aciworldwide.aclabs26.repository.AccountRepository;
import com.aciworldwide.aclabs26.repository.TransactionRepository;
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
    void shouldReturnDailyFrequencyLimitExceeded() throws Exception {
        AccountModel account = accountRepository.findByCardNumber("1234567812345678");
        account.setDailyTxLimit(0);
        account.setDailyTx(1);
        accountRepository.save(account);

        mockMvc.perform(post("/transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cardNumber\":\"1234567812345678\",\"amount\":10.0,\"timestamp\":\"2026-05-03T10:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("65")))
                .andExpect(jsonPath("$.businessLogicViolation", is(true)));
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
