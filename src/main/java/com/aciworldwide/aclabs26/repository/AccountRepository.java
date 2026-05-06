package com.aciworldwide.aclabs26.repository;

import com.aciworldwide.aclabs26.entities.AccountModel;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.UUID;

@Repository
public interface AccountRepository extends CrudRepository<AccountModel, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    AccountModel findAccountByCardNumber(String cardNumber);

    boolean existsAccountByCardNumber(String cardNumber);

}
