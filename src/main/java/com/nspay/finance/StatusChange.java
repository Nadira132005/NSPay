package com.nspay.finance;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.persistence.*;
import java.time.Instant;
@Data @EqualsAndHashCode(callSuper=true) @Entity @Table(name="STATUS_CHANGES")
public class StatusChange extends OwnedRecord {
    private long actorId;
    private String previousStatus;
    private String newStatus;
    private String reason;
    private Instant suspendedUntil;
    private int suspensionCount;
    @Column(length=250) private String publicNote;
    @Column(length=250) private String privateNote;
    private Integer misalignmentScore;
    private Long transactionId;
}
