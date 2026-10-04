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
@Table(name="SAVINGS_POCKETS")
public class SavingsPocket extends OwnedRecord {
    private String name;
    @Column(precision=19, scale=2) private BigDecimal balance = BigDecimal.ZERO;
    @Column(precision=19, scale=2) private BigDecimal targetAmount;
}
