package com.nspay.controllers;

import com.nspay.entities.Account;
import com.nspay.dto.AccountDTO;
import com.nspay.exceptions.GlobalExceptionHandler;
import com.nspay.services.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class) //controller-only tests
@Import(GlobalExceptionHandler.class)
//we mock the repository and we test the endpoints
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;
    @MockBean private com.nspay.finance.AccountLimitsService limits;

    @Test
    void shouldReturnAllAccounts() throws Exception {
        List<Account> accounts = Arrays.asList(
                new Account(1L, "1234000000056789", "John Black", 15.00, 1000000, 1000000000.00, 0, 0.00),
                new Account(2L, "1111222233334444", "Lara Dean", 89.00, 1000000, 1000000000.00, 0, 0.00),
                new Account(3L, "4321000000056789", "Alice Bond", 1143.00, 1000000, 1000000000.00, 0, 0.00)
        );

        ResponseEntity<?> response = new ResponseEntity<>(accounts, new HttpHeaders(), HttpStatus.OK);
        doReturn(response).when(accountService).getAllAccounts();

        mockMvc.perform(get("/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].cardHolderName", is("John Black")))
                .andExpect(jsonPath("$[1].cardHolderName", is("Lara Dean")))
                .andExpect(jsonPath("$[2].cardHolderName", is("Alice Bond")));
    }

    @Test
    void shouldReturnAccountById() throws Exception {
        Account account = new Account(1L, "1234000000056789", "John Black", 15.00, 1000000, 1000000000.00, 0, 0.00);
        ResponseEntity<?> response = new ResponseEntity<>(account, new HttpHeaders(), HttpStatus.OK);
        doReturn(response).when(accountService).getAccountById(1L);

        mockMvc.perform(get("/accounts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardHolderName", is("John Black")))
                .andExpect(jsonPath("$.cardNumber", is("1234000000056789")));
    }

    @Test
    void shouldReturnAccountByCardNumber() throws Exception {
        Account account = new Account(2L, "1111222233334444", "Lara Dean", 89.00, 1000000, 1000000000.00, 0, 0.00);
        ResponseEntity<?> response = new ResponseEntity<>(account, new HttpHeaders(), HttpStatus.OK);
        doReturn(response).when(accountService).getAccountByCardNumber("1111222233334444");

        mockMvc.perform(get("/accounts/card-number/1111222233334444"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardHolderName", is("Lara Dean")))
                .andExpect(jsonPath("$.cardNumber", is("1111222233334444")));
    }

    @Test
    void shouldAddAccount() throws Exception {
        Account account = new Account(4L, "9999888877776666", "Bob Stone", 25.00, 1000000, 1000000000.00, 0, 0.00);
        ResponseEntity<?> response = new ResponseEntity<>(account, new HttpHeaders(), HttpStatus.CREATED);
        doReturn(response).when(accountService).addAccount(org.mockito.ArgumentMatchers.any(AccountDTO.class));

        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cardNumber\":\"9999888877776666\",\"cardHolderName\":\"Bob Stone\",\"amount\":25.0,\"dailyTxLimit\":1000000,\"dailyTxSumLimit\":1000000000.0,\"dailyTx\":0,\"dailyTxSum\":0.0}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cardHolderName", is("Bob Stone")))
                .andExpect(jsonPath("$.cardNumber", is("9999888877776666")));
    }

    @Test
    void shouldUpdateAccount() throws Exception {
        doReturn(java.util.Map.of("id",1,"usage",java.util.Map.of("dailyTxLimit",1000000))).when(limits).update(org.mockito.ArgumentMatchers.eq(42L),org.mockito.ArgumentMatchers.eq(1L),org.mockito.ArgumentMatchers.any(com.nspay.finance.AccountLimitsService.Limits.class));
        mockMvc.perform(put("/accounts/1").requestAttr("authenticatedUserId",42L)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"cardNumber\":\"1234000000056789\",\"cardHolderName\":\"John Black Updated\",\"amount\":30.0,\"dailyTxLimit\":1000000,\"dailyTxSumLimit\":1000000000.0,\"dailyTx\":0,\"dailyTxSum\":0.0}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.usage.dailyTxLimit",is(1000000)));
    }

    @Test
    void shouldDeleteAccount() throws Exception {
        ResponseEntity<?> response = new ResponseEntity<>(HttpStatus.NO_CONTENT);
        org.mockito.Mockito.doThrow(new org.springframework.web.server.ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED,"Account deletion is unavailable")).when(accountService).deleteAccount(1L);

        mockMvc.perform(delete("/accounts/1"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void shouldReturnBadRequestForInvalidAccount() throws Exception {
        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cardHolderName\":\"\",\"amount\":0,\"dailyTxLimit\":0,\"dailyTxSumLimit\":0,\"dailyTx\":0,\"dailyTxSum\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Card number is required")));
    }

}
