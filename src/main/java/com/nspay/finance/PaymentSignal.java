package com.nspay.finance;

import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Data @EqualsAndHashCode(callSuper=true) @Entity
@Table(name="PAYMENT_SIGNALS",uniqueConstraints=@UniqueConstraint(columnNames="transactionId"),indexes={@Index(name="idx_payment_score",columnList="misalignmentScore"),@Index(name="idx_payment_review",columnList="reviewStatus,paidAt"),@Index(name="idx_payment_owner",columnList="userId,reviewStatus,id")})
public class PaymentSignal extends OwnedRecord {
    private long transactionId;
    private String username;
    private String recipientIban;
    private String transactionType;
    private Instant paidAt;
    @Column(precision=19,scale=2) private BigDecimal amount;
    private int amountScore;
    private int timingScore;
    private int velocityScore;
    private int recipientScore;
    private int reportingScore;
    private int misalignmentScore;
    private int limitScore;private int dailyTx;private double dailyTxSum;private int dailyTxLimit;private double dailyTxSumLimit;
    private int historySamples;
    private int reportCount;
    private boolean reported;
    private String reviewStatus="OPEN";
    private Long reviewedBy;
    private Instant reviewedAt;
    @Column(length=250) private String publicNote;
    @Column(length=250) private String privateNote;
    private String modelVersion="payment-signals-v2";
    private Integer decisionScore;
}
