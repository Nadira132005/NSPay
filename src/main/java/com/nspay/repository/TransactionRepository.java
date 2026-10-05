package com.nspay.repository;

import com.nspay.entities.TransactionModel;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

@Transactional
@Repository
public interface TransactionRepository extends org.springframework.data.jpa.repository.JpaRepository<TransactionModel, Long> {

    List<TransactionModel> findAllByCardNumber(String cardNumber);
    @org.springframework.data.jpa.repository.Query("select count(t), coalesce(sum(t.amount),0) from TransactionModel t where t.cardNumber=:card and t.returnCode='00' and (t.transactionType is null or t.transactionType='CARD_PAYMENT' or t.transactionType='PAYMENT' or t.transactionType='TRANSFER_OUT') and t.timestamp>=:start and (t.timestamp<:at or (t.timestamp=:at and t.id<=:id))")
    List<Object[]> dailyPaymentUsage(@org.springframework.data.repository.query.Param("card") String card,@org.springframework.data.repository.query.Param("start") java.sql.Timestamp start,@org.springframework.data.repository.query.Param("at") java.sql.Timestamp at,@org.springframework.data.repository.query.Param("id") long id);
    @org.springframework.data.jpa.repository.Query("select t from TransactionModel t, AccountModel a, UserModel u where t.cardNumber=a.cardNumber and u.accountId=a.id and not exists (select s.id from PaymentSignal s where s.transactionId=t.id) order by t.id")
    List<TransactionModel> unscored(org.springframework.data.domain.Pageable page);
    @org.springframework.data.jpa.repository.Query("select t from TransactionModel t where t.cardNumber=:card and t.timestamp<:before and t.returnCode='00' order by t.timestamp desc")
    List<TransactionModel> scoreHistory(@org.springframework.data.repository.query.Param("card") String card,@org.springframework.data.repository.query.Param("before") java.sql.Timestamp before,org.springframework.data.domain.Pageable page);
}
