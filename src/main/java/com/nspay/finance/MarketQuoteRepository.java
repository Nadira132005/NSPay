package com.nspay.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
public interface MarketQuoteRepository extends JpaRepository<MarketQuote,String> {
    List<MarketQuote> findByProviderAndExpiresAtAfter(String provider,Instant now);
    @Transactional @Modifying @Query("delete from MarketQuote q where q.expiresAt < :before")
    int deleteExpired(@org.springframework.data.repository.query.Param("before") Instant before);
}
