package com.nspay.repository;

import com.nspay.entities.UserModel;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.CrudRepository;

import javax.persistence.LockModeType;

public interface UserRepository extends CrudRepository<UserModel, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    UserModel findUserModelByNameAndPassword(String name, String password);

    UserModel findByName(String name);
    java.util.Optional<UserModel> findByAccountId(Long accountId);
    boolean existsByName(String name);
    boolean existsByAccountId(Long accountId);
    boolean existsByAccountIdAndIdNot(Long accountId, long id);

}
