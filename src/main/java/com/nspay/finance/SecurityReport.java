package com.nspay.finance;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.persistence.*;
import java.time.Instant;
@Data @EqualsAndHashCode(callSuper=true) @Entity
@Table(name="SECURITY_REPORTS", uniqueConstraints=@UniqueConstraint(columnNames="transactionId"))
public class SecurityReport extends OwnedRecord {
    private long transactionId;
    private long reporterId;
    private String reason;
    private String status="OPEN";
    private String decisionNote;
    private Long reviewedBy;
    private Instant reviewedAt;
    @Column(length=250) private String publicNote;
    @Column(length=250) private String privateNote;
}
