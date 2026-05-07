package com.aciworldwide.aclabs26.repository;

import com.aciworldwide.aclabs26.entities.UserModel;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.CrudRepository;

import javax.persistence.LockModeType;

public interface UserRepository extends CrudRepository<UserModel, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    UserModel findUserModelByNameAndPassword(String name, String password);

}
