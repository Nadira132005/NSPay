package com.nspay.finance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import javax.persistence.LockModeType;
import java.util.*;
import java.time.Instant;
public interface ScheduledTransferRepository extends JpaRepository<ScheduledTransfer, Long> {
    List<ScheduledTransfer> findByUserIdOrderByCreatedAtDesc(long userId);
    Optional<ScheduledTransfer> findByIdAndUserId(long id, long userId);
    List<ScheduledTransfer> findTop50ByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(String status, Instant time);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ScheduledTransfer r where r.id = :id")
    Optional<ScheduledTransfer> lockById(@Param("id") long id);
}
