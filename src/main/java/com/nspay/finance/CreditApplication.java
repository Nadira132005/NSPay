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
@Table(name="CREDIT_APPLICATIONS")
public class CreditApplication extends OwnedRecord {
    @Column(precision=19, scale=2) private BigDecimal amount;
    @Column(precision=19, scale=2) private BigDecimal monthlyIncome;
    private int termMonths;
    private String purpose;
    private String status = "DRAFT";
    private String reviewNote;
}
