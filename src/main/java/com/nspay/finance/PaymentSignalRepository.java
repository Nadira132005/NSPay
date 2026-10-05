package com.nspay.finance;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface PaymentSignalRepository extends JpaRepository<PaymentSignal,Long>,JpaSpecificationExecutor<PaymentSignal> {
    java.util.List<PaymentSignal> findByModelVersion(String version,org.springframework.data.domain.Pageable page);
    org.springframework.data.domain.Slice<PaymentSignal> findByUserIdAndReviewStatus(long userId,String state,org.springframework.data.domain.Pageable page);
    Optional<PaymentSignal> findByTransactionId(long transactionId);
    @Lock(javax.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from PaymentSignal s where s.transactionId=:id")
    Optional<PaymentSignal> lockByTransactionId(@Param("id") long id);
    @Modifying
    @Query("update PaymentSignal s set s.reportCount=:count, s.reportingScore=case when s.reported=true and s.reviewStatus<>'DISMISSED' and :score<60 then 60 else :score end, s.misalignmentScore=function('least',100,function('round',s.amountScore*0.35+s.timingScore*0.15+s.velocityScore*0.15+s.recipientScore*0.15+(case when s.reported=true and s.reviewStatus<>'DISMISSED' and :score<60 then 60 else :score end)*0.20+s.limitScore*0.25)) where s.recipientIban=:iban")
    int updateRecipientReports(@Param("iban") String iban,@Param("count") int count,@Param("score") int score);
}
