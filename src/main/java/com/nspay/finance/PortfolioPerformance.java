package com.nspay.finance;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
@Data @EqualsAndHashCode(callSuper=true) @Entity
@Table(name="PORTFOLIO_PERFORMANCE",uniqueConstraints=@UniqueConstraint(columnNames={"userId","day"}))
public class PortfolioPerformance extends OwnedRecord {
    @Column(nullable=false) private LocalDate day;
    @Column(nullable=false,precision=19,scale=2) private BigDecimal totalGain;
    @Column(nullable=false,precision=19,scale=2) private BigDecimal realizedGain;
    @Column(nullable=false,precision=19,scale=2) private BigDecimal unrealizedGain;
    @Column(nullable=false,precision=19,scale=2) private BigDecimal holdingsValue;
    @Column(nullable=false) private long walletVersion;
}
