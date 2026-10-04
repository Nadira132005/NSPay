package com.nspay.finance;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.persistence.*;
import java.time.Instant;
@Data @EqualsAndHashCode(callSuper=true) @Entity
@Table(name="ACCOUNT_RESTRICTIONS", uniqueConstraints=@UniqueConstraint(columnNames="userId"))
public class AccountRestriction extends OwnedRecord {
    private int suspensionCount;
    private Instant suspendedUntil;
    private String reason;
}
