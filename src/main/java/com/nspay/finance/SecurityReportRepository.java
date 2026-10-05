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
    @org.springframework.data.jpa.repository.Query("select count(distinct r.reporterId) from SecurityReport r, com.nspay.entities.TransactionModel t where r.transactionId=t.id and t.counterpartyIban=:iban and r.status<>'CLEARED'")
    long recipientReporters(@org.springframework.data.repository.query.Param("iban") String iban);
    @org.springframework.data.jpa.repository.Query("select r from SecurityReport r, com.nspay.entities.TransactionModel t where r.transactionId=t.id and (r.transactionId=:transaction or (:iban is not null and t.counterpartyIban=:iban)) order by r.createdAt desc")
    List<SecurityReport> evidence(@org.springframework.data.repository.query.Param("transaction") long transaction,@org.springframework.data.repository.query.Param("iban") String iban,org.springframework.data.domain.Pageable page);
}
