package com.nspay.finance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import javax.persistence.LockModeType;
import java.util.*;
import java.time.Instant;
public interface TradeOrderRepository extends JpaRepository<TradeOrder, Long> {
    org.springframework.data.domain.Page<TradeOrder> findByUserId(long userId,org.springframework.data.domain.Pageable pageable);
    @Query("select t from TradeOrder t where t.userId=:userId and (t.createdAt>:after or (t.createdAt=:after and t.id>:afterId)) order by t.createdAt,t.id")
    List<TradeOrder> findFollowing(@Param("userId") long userId,@Param("after") Instant after,@Param("afterId") long afterId,org.springframework.data.domain.Pageable pageable);
    List<TradeOrder> findByUserIdOrderByCreatedAtDesc(long userId);
    Optional<TradeOrder> findByIdAndUserId(long id, long userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from TradeOrder r where r.id = :id")
    Optional<TradeOrder> lockById(@Param("id") long id);
}
