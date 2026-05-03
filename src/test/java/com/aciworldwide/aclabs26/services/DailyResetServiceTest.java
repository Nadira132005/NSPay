package com.aciworldwide.aclabs26.services;

import com.aciworldwide.aclabs26.entities.AccountModel;
import com.aciworldwide.aclabs26.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyResetServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @InjectMocks //for the class i want to test
    private DailyResetService dailyResetService;

    @Test
    void shouldResetDailyCountersForAllAccounts() {
        AccountModel first = new AccountModel();
        AccountModel second = new AccountModel();
        first.setDailyTx(5);
        first.setDailyTxSum(100.0);
        second.setDailyTx(3);
        second.setDailyTxSum(50.0);
        when(accountRepository.findAll()).thenReturn(Arrays.asList(first, second));//creates stubs
        dailyResetService.resetDailyCounters();
        assertThat(first.getDailyTx()).isZero();
        assertThat(first.getDailyTxSum()).isZero();
        assertThat(second.getDailyTx()).isZero();
        assertThat(second.getDailyTxSum()).isZero();
    }
}