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
@Table(name="CREDIT_DOCUMENTS")
public class CreditDocument extends OwnedRecord {
    private Long applicationId;
    private String kind;
    private String filename;
    private String contentType;
    private String sha256;
    @JsonIgnore @Lob @Column(columnDefinition="LONGBLOB") private byte[] content;
    private String scanStatus;
    @Column(length=4000) private String extractedText;
}
