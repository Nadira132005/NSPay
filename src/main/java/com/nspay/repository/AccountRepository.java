package com.nspay.repository;

import com.nspay.entities.AccountModel;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import javax.transaction.Transactional;

@Transactional
@Repository
public interface AccountRepository extends CrudRepository<AccountModel, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    AccountModel findByCardNumber(String cardNumber);

    boolean existsByCardNumber(String cardNumber);
    AccountModel findByIban(String iban);
    boolean existsByIban(String iban);

    @org.springframework.data.jpa.repository.Modifying
    @Query("update AccountModel a set a.dailyTx=0, a.dailyTxSum=0")
    int resetDailyCounters();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AccountModel a where a.id = :id")
    AccountModel lockById(@Param("id") long id);

}
