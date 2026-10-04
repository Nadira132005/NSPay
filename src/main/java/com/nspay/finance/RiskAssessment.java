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
@Table(name="RISK_ASSESSMENTS")
public class RiskAssessment extends OwnedRecord {
    @Column(precision=19, scale=2) private BigDecimal amount;
    private String recipientIban;
    private int score;
    private String level;
    @Column(length=2000) private String reasons;
    private String modelVersion = "local-behavioral-v1";
    private int historySamples;
    private String status = "OPEN";
}
