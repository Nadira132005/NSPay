package com.nspay.finance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import javax.persistence.LockModeType;
import java.util.*;
import java.time.Instant;
public interface TradeOrderRepository extends JpaRepository<TradeOrder, Long> {
    List<TradeOrder> findByUserIdOrderByCreatedAtDesc(long userId);
    Optional<TradeOrder> findByIdAndUserId(long id, long userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from TradeOrder r where r.id = :id")
    Optional<TradeOrder> lockById(@Param("id") long id);
}
