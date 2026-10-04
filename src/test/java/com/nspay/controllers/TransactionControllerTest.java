package com.nspay.controllers;

import com.nspay.exceptions.GlobalExceptionHandler;
import com.nspay.entities.TransactionModel;
import com.nspay.services.AccountService;
import com.nspay.services.TransactionService;
import com.nspay.services.TransferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

    @MockBean
    private TransferService transferService;

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

    @Test
    void shouldReturnAllTransactions() throws Exception {
        TransactionModel first = new TransactionModel(1L, "1234567812345678", 25.0, Timestamp.valueOf("2026-05-03 10:00:00"), "00");
        TransactionModel second = new TransactionModel(2L, "1234567812345678", 10.0, Timestamp.valueOf("2026-05-03 10:05:00"), "51");
        doReturn(Arrays.asList(first, second)).when(transactionService).getAllTransactions();

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].cardNumber", is("1234567812345678")))
                .andExpect(jsonPath("$[1].returnCode", is("51")));
    }

    @Test
    void shouldReturnTransactionsByCardNumber() throws Exception {
        TransactionModel first = new TransactionModel(1L, "1234567812345678", 25.0, Timestamp.valueOf("2026-05-03 10:00:00"), "00");
        doReturn(Arrays.asList(first)).when(transactionService).getTransactionsOfAccount("1234567812345678");

        mockMvc.perform(get("/transactions/1234567812345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].amount", is(25.0)));
    }
}
