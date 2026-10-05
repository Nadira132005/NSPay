package com.nspay.finance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import javax.persistence.LockModeType;
import java.util.*;
import java.time.Instant;
public interface CreditDocumentRepository extends JpaRepository<CreditDocument, Long> {
    List<CreditDocument> findByUserIdOrderByCreatedAtDesc(long userId);
    Optional<CreditDocument> findByIdAndUserId(long id, long userId);
    List<CreditDocument> findByApplicationIdAndUserId(long applicationId, long userId);
    List<CreditDocument> findByUserIdAndApplicationIdIsNullOrderByCreatedAtAsc(long userId);
    boolean existsByUserIdAndApplicationIdIsNullAndKind(long userId,String kind);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from CreditDocument r where r.id = :id")
    Optional<CreditDocument> lockById(@Param("id") long id);
}
