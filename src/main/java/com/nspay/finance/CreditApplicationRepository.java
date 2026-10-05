package com.nspay.finance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import javax.persistence.LockModeType;
import java.util.*;
import java.time.Instant;
public interface CreditApplicationRepository extends JpaRepository<CreditApplication, Long>,org.springframework.data.jpa.repository.JpaSpecificationExecutor<CreditApplication> {
    List<CreditApplication> findByUserIdOrderByCreatedAtDesc(long userId);
    Optional<CreditApplication> findByIdAndUserId(long id, long userId);
    List<CreditApplication> findTop100ByOrderByCreatedAtDesc();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from CreditApplication r where r.id = :id")
    Optional<CreditApplication> lockById(@Param("id") long id);
}
