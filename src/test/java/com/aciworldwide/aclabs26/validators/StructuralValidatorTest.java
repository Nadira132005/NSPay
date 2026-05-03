package com.aciworldwide.aclabs26.validators;

import com.aciworldwide.aclabs26.dto.TransactionDTO;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.assertj.core.api.Assertions.assertThat;

class StructuralValidatorTest {

    private final StructuralValidator structuralValidator = new StructuralValidator();

    @Test
    void shouldRejectMissingTimestamp() {
        TransactionDTO dto = new TransactionDTO("1234567812345678", 10.0, null);

        ValidationResults results = structuralValidator.validate(dto);

        assertThat(results.isValid()).isFalse();
        assertThat(results.getCode()).isEqualTo(ResponseCode.INVALID_CARD_NUMBER);
    }

    @Test
    void shouldAcceptValidTransactionPayload() {
        TransactionDTO dto = new TransactionDTO("1234567812345678", 10.0, Timestamp.valueOf("2026-05-03 10:00:00"));

        ValidationResults results = structuralValidator.validate(dto);

        assertThat(results.isValid()).isTrue();
        assertThat(results.getCode()).isEqualTo(ResponseCode.APPROVED);
    }
}
