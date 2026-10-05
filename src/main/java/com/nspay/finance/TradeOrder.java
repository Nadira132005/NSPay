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
@Table(name="TRADE_ORDERS",indexes=@Index(name="idx_trade_owner_time",columnList="userId,createdAt,id"))
public class TradeOrder extends OwnedRecord {
    private String symbol;
    private String side;
    @Column(precision=19, scale=6) private BigDecimal quantity;
    @Column(precision=19, scale=6) private BigDecimal price;
    @Column(precision=19, scale=2) private BigDecimal total;
    private String status = "FILLED";
    @Column(precision=19,scale=6) private BigDecimal marketPrice;
    @Column(precision=19,scale=8) private BigDecimal exchangeRate;
    private Instant priceAt;
    private Instant exchangeRateAt;
    private String quoteId;
    @Column(nullable=false,length=32) private String provider="twelve-data";
}
