package com.nspay.finance;
import lombok.Data;
import javax.persistence.*;
import java.time.Instant;
@Data
@MappedSuperclass
public abstract class OwnedRecord {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private long userId;
    @Column(nullable=false) private Instant createdAt = Instant.now();
    @Version private long version;
}
