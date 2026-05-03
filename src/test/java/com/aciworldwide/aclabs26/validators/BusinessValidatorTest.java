package com.aciworldwide.aclabs26.validators;

import com.aciworldwide.aclabs26.dto.TransactionDTO;
import com.aciworldwide.aclabs26.entities.AccountModel;
import com.aciworldwide.aclabs26.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessValidatorTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private BusinessValidator businessValidator;

    @Test
    void shouldRejectMissingAccount() {
        TransactionDTO dto = new TransactionDTO("1234567812345678", 10.0, Timestamp.valueOf("2026-05-03 10:00:00"));
        when(accountRepository.findByCardNumber("1234567812345678")).thenReturn(null);

        ValidationResults results = businessValidator.validate(dto);

        assertThat(results.isValid()).isFalse();
        assertThat(results.getCode()).isEqualTo(ResponseCode.INVALID_CARD_NUMBER);
    }

    @Test
    void shouldDetectInsufficientFunds() {
        TransactionDTO dto = new TransactionDTO("1234567812345678", 250.0, Timestamp.valueOf("2026-05-03 10:00:00"));
        AccountModel account = new AccountModel();
        account.setCardNumber("1234567812345678");
        account.setAmount(100.0);
        account.setDailyTxLimit(10);
        account.setDailyTxSumLimit(1000.0);
        when(accountRepository.findByCardNumber("1234567812345678")).thenReturn(account);

        ValidationResults results = businessValidator.validate(dto);

        assertThat(results.isValid()).isTrue();
        assertThat(results.isBusinessLogicViolation()).isTrue();
        assertThat(results.getCode()).isEqualTo("51");
    }

    @Test
    void shouldAcceptValidBusinessRules() {
        TransactionDTO dto = new TransactionDTO("1234567812345678", 10.0, Timestamp.valueOf("2026-05-03 10:00:00"));
        AccountModel account = new AccountModel();
        account.setCardNumber("1234567812345678");
        account.setAmount(100.0);
        account.setDailyTxLimit(10);
        account.setDailyTxSumLimit(1000.0);
        account.setDailyTx(0);
        account.setDailyTxSum(0.0);
        when(accountRepository.findByCardNumber("1234567812345678")).thenReturn(account);

        ValidationResults results = businessValidator.validate(dto);

        assertThat(results.isValid()).isTrue();
        assertThat(results.isBusinessLogicViolation()).isFalse();
        assertThat(results.getCode()).isEqualTo(ResponseCode.APPROVED);
    }
}
