package com.nspay.finance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface SecurityReportRepository extends JpaRepository<SecurityReport,Long> {
    @org.springframework.data.jpa.repository.Lock(javax.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<SecurityReport> findByTransactionId(long id);
    List<SecurityReport> findByUserIdOrderByCreatedAtDesc(long id);
    @org.springframework.data.jpa.repository.Lock(javax.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from SecurityReport r where r.userId=:id and r.status='OPEN'")
    List<SecurityReport> openReports(@org.springframework.data.repository.query.Param("id") long id);
    List<SecurityReport> findTop100ByOrderByCreatedAtDesc();
}
