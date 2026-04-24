package com.aciworldwide.aclabs26.controllers;

import com.aciworldwide.aclabs26.exceptions.GlobalExceptionHandler;
import com.aciworldwide.aclabs26.services.AccountService;
import com.aciworldwide.aclabs26.services.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@Import(GlobalExceptionHandler.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private AccountService accountService;

    @Test
    void shouldReturnInvalidAmount() throws Exception {
        mockMvc.perform(post("/transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cardNumber\":\"123\",\"amount\":-10}"))
                .andExpect(content().string(containsString("amount: Amount must be greater than zero")));
    }

    @Test
    void shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cardNumber\":\"\",\"amount\":2}"))
                .andExpect(status().isBadRequest());

    }
}
