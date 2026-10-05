package com.nspay.finance;

import lombok.Data;
import javax.persistence.*;
import java.time.Instant;

/** Immutable executable price snapshot. Shared storage lets any backend instance accept its ID. */
@Data @Entity
@Table(name="MARKET_QUOTES",indexes={@Index(name="idx_quote_lookup",columnList="provider,symbol,expiresAt"),@Index(name="idx_quote_expiry",columnList="expiresAt")})
public class MarketQuote {
    @Id @Column(length=36) private String id;
    @Column(nullable=false,length=32) private String provider;
    @Column(nullable=false,length=16) private String symbol;
    @Column(nullable=false) private Instant expiresAt;
    @Lob @Column(nullable=false,columnDefinition="LONGTEXT") private String payload;
}
