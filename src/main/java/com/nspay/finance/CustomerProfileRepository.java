package com.nspay.finance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import javax.persistence.LockModeType;
import java.util.*;
import java.time.Instant;
public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {
    List<CustomerProfile> findByUserIdOrderByCreatedAtDesc(long userId);
    Optional<CustomerProfile> findByIdAndUserId(long id, long userId);
    Optional<CustomerProfile> findByUserId(long userId);
    List<CustomerProfile> findTop100ByOrderByCreatedAtDesc();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from CustomerProfile r where r.userId = :userId")
    Optional<CustomerProfile> lockByUserId(@Param("userId") long userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from CustomerProfile r where r.id = :id")
    Optional<CustomerProfile> lockById(@Param("id") long id);
}
