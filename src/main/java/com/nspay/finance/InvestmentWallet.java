package com.nspay.finance;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.persistence.*;
import java.math.BigDecimal;
@Data @EqualsAndHashCode(callSuper=true) @Entity
@Table(name="INVESTMENT_WALLETS",uniqueConstraints=@UniqueConstraint(columnNames="userId"))
public class InvestmentWallet extends OwnedRecord {
    @Column(nullable=false,precision=19,scale=2) private BigDecimal balance=BigDecimal.ZERO;
    @com.fasterxml.jackson.annotation.JsonIgnore @Column(nullable=false) private boolean positionsInitialized;
}
