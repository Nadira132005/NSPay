package com.aciworldwide.aclabs26.controllers;

import com.aciworldwide.aclabs26.entities.Account;
import com.aciworldwide.aclabs26.services.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

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
}
