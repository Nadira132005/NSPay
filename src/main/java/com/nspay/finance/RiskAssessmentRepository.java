package com.nspay.finance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import javax.persistence.LockModeType;
import java.util.*;
import java.time.Instant;
public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, Long> {
    List<RiskAssessment> findByUserIdOrderByCreatedAtDesc(long userId);
    Optional<RiskAssessment> findByIdAndUserId(long id, long userId);
    List<RiskAssessment> findTop100ByOrderByCreatedAtDesc();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RiskAssessment r where r.id = :id")
    Optional<RiskAssessment> lockById(@Param("id") long id);
}
