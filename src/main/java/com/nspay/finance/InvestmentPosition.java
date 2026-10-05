package com.nspay.finance;

import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.persistence.*;
import java.math.BigDecimal;

/** Average-cost accounting projection, updated in the same transaction as cash and orders. */
@Data @EqualsAndHashCode(callSuper=true) @Entity
@Table(name="INVESTMENT_POSITIONS", uniqueConstraints=@UniqueConstraint(columnNames={"userId","provider","symbol"}))
public class InvestmentPosition extends OwnedRecord {
    @Column(nullable=false,length=32) private String provider;
    @Column(nullable=false,length=16) private String symbol;
    @Column(nullable=false,precision=19,scale=6) private BigDecimal quantity=BigDecimal.ZERO;
    @Column(nullable=false,precision=25,scale=8) private BigDecimal costBasis=BigDecimal.ZERO;
    @Column(nullable=false,precision=25,scale=8) private BigDecimal realizedGain=BigDecimal.ZERO;
}
