package com.nspay.finance;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.persistence.*;
@Data @EqualsAndHashCode(callSuper=true) @Entity
@Table(name="INVESTMENT_COMMANDS",uniqueConstraints=@UniqueConstraint(columnNames={"userId","requestKey"}))
public class InvestmentCommand extends OwnedRecord {
    private String requestKey;
    private String fingerprint;
    private Long orderId;
}
