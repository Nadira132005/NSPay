package com.aciworldwide.aclabs26.controllers;

import com.aciworldwide.aclabs26.entities.AccountModel;
import com.aciworldwide.aclabs26.entities.AccountStatus;
import com.aciworldwide.aclabs26.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
@SpringBootTest //controller + service + repository, full integration tests
@AutoConfigureMockMvc
class AccountControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
        accountRepository.save(createAccount("1234000000056789", "John Black", 15.0));
        accountRepository.save(createAccount("1111222233334444", "Lara Dean", 89.0));
        accountRepository.save(createAccount("4321000000056789", "Alice Bond", 1143.0));
    }

    @Test
    void shouldReturnAllAccounts() throws Exception {
        mockMvc.perform(get("/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3))) //$-the entire JSON object returned in the HTTP response body
                .andExpect(jsonPath("$[0].cardHolderName", is("John Black")))
                .andExpect(jsonPath("$[1].cardHolderName", is("Lara Dean")))
                .andExpect(jsonPath("$[2].cardHolderName", is("Alice Bond")));
    }

    @Test
    void shouldReturnAccountById() throws Exception {
        AccountModel account = accountRepository.findByCardNumber("1234000000056789");

        mockMvc.perform(get("/accounts/" + account.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardHolderName", is("John Black")))
                .andExpect(jsonPath("$.cardNumber", is("1234000000056789")));
    }

    @Test
    void shouldReturnAccountByCardNumber() throws Exception {
        mockMvc.perform(get("/accounts/card-number/1111222233334444"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardHolderName", is("Lara Dean")))
                .andExpect(jsonPath("$.cardNumber", is("1111222233334444")));
    }

    @Test
    void shouldAddAccount() throws Exception {
        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cardNumber\":\"4242424242424242\",\"cardHolderName\":\"Bob Stone\",\"amount\":25.0,\"dailyTxLimit\":1000000,\"dailyTxSumLimit\":1000000000.0,\"dailyTx\":0,\"dailyTxSum\":0.0}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cardHolderName", is("Bob Stone")))
                .andExpect(jsonPath("$.cardNumber", is("4242424242424242")));
    }

    @Test
    void shouldRejectAccountWithInvalidLuhnCardNumber() throws Exception {
        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cardNumber\":\"9999888877776666\",\"cardHolderName\":\"Bob Stone\",\"amount\":25.0,\"dailyTxLimit\":1000000,\"dailyTxSumLimit\":1000000000.0,\"dailyTx\":0,\"dailyTxSum\":0.0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpdateAccount() throws Exception {
        AccountModel account = accountRepository.findByCardNumber("1234000000056789");

        mockMvc.perform(put("/accounts/" + account.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cardNumber\":\"1234000000056789\",\"cardHolderName\":\"John Black Updated\",\"amount\":30.0,\"dailyTxLimit\":1000000,\"dailyTxSumLimit\":1000000000.0,\"dailyTx\":0,\"dailyTxSum\":0.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardHolderName", is("John Black Updated")))
                .andExpect(jsonPath("$.amount", is(30.0)));
    }

    @Test
    void shouldDeleteAccount() throws Exception {
        AccountModel account = accountRepository.findByCardNumber("1234000000056789");

        mockMvc.perform(delete("/accounts/" + account.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/accounts/" + account.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnNotFoundForMissingAccount() throws Exception {
        mockMvc.perform(get("/accounts/19999"))
                .andExpect(status().isNotFound());
    }

    private AccountModel createAccount(String cardNumber, String cardHolderName, double amount) {
        AccountModel account = new AccountModel();
        account.setCardNumber(cardNumber);
        account.setCardHolderName(cardHolderName);
        account.setStatus(AccountStatus.ACTIVE);
        account.setAmount(amount);
        account.setDailyTxLimit(1000000);
        account.setDailyTxSumLimit(1000000000.0);
        account.setDailyTx(0);
        account.setDailyTxSum(0.0);
        return account;
    }
}
