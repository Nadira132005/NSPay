package com.aciworldwide.aclabs26.repository;

import com.aciworldwide.aclabs26.entities.AccountModel;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import javax.transaction.Transactional;

@Transactional
@Repository
public interface AccountRepository extends CrudRepository<AccountModel, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    AccountModel findByCardNumber(String cardNumber);

    boolean existsByCardNumber(String cardNumber);
}
