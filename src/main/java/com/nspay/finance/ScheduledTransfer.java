package com.nspay.finance;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonIgnore;
@Data
@EqualsAndHashCode(callSuper=true)
@Entity
@Table(name="SCHEDULED_TRANSFERS")
public class ScheduledTransfer extends OwnedRecord {
    private String recipientIban;
    @Column(precision=19, scale=2) private BigDecimal amount;
    private Instant scheduledAt;
    private String status = "PENDING";
    private String failureReason;
    private Instant executedAt;
}
