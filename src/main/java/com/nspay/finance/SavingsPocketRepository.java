package com.nspay.finance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import javax.persistence.LockModeType;
import java.util.*;
import java.time.Instant;
public interface SavingsPocketRepository extends JpaRepository<SavingsPocket, Long> {
    List<SavingsPocket> findByUserIdOrderByCreatedAtDesc(long userId);
    Optional<SavingsPocket> findByIdAndUserId(long id, long userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from SavingsPocket r where r.id = :id")
    Optional<SavingsPocket> lockById(@Param("id") long id);
}
