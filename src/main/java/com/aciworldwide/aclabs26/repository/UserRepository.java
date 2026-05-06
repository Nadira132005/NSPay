package com.aciworldwide.aclabs26.repository;

import com.aciworldwide.aclabs26.entities.UserModel;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.CrudRepository;

import javax.persistence.LockModeType;
import java.util.UUID;

public interface UserRepository extends CrudRepository<UserModel, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    UserModel findUserModelByNameAndPassword(String name, String password);

}
