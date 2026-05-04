package com.aciworldwide.aclabs26.mappers;
import com.aciworldwide.aclabs26.dto.TransactionDTO;
import com.aciworldwide.aclabs26.entities.TransactionModel;
import org.junit.jupiter.api.Test;
import java.sql.Timestamp;
import static org.assertj.core.api.Assertions.assertThat;

class TransactionMapperTest {

    private final TransactionMapper transactionMapper = new TransactionMapper();

    @Test
    void shouldMapDtoToModel() {
        TransactionDTO dto = new TransactionDTO("1234567812345678", 25.0, Timestamp.valueOf("2026-05-03 10:00:00"));

        TransactionModel model = transactionMapper.toModel(dto);

        assertThat(model.getCardNumber()).isEqualTo("1234567812345678");
        assertThat(model.getAmount()).isEqualTo(25.0);
        assertThat(model.getTimestamp()).isEqualTo(Timestamp.valueOf("2026-05-03 10:00:00"));
    }

    @Test
    void shouldMapModelToDto() {
        TransactionModel model = new TransactionModel();
        model.setCardNumber("1234567812345678");
        model.setAmount(25.0);
        model.setTimestamp(Timestamp.valueOf("2026-05-03 10:00:00"));

        TransactionDTO dto = transactionMapper.toDto(model);

        assertThat(dto.getCardNumber()).isEqualTo("1234567812345678");
        assertThat(dto.getAmount()).isEqualTo(25.0);
        assertThat(dto.getTimestamp()).isEqualTo(Timestamp.valueOf("2026-05-03 10:00:00"));
    }
}
