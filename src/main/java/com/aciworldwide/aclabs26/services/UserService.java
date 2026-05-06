package com.aciworldwide.aclabs26.services;

import com.aciworldwide.aclabs26.entities.UserModel;
import com.aciworldwide.aclabs26.repository.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.transaction.Transactional;

@Service
@Log4j2
public class UserService {

    @Autowired
    UserRepository userRepository;

    @Transactional
    public UserModel loginUser(UserModel userModel) {
        log.info( userRepository.findUserModelByNameAndPassword(userModel.getName(), userModel.getPassword()));
        return userRepository.findUserModelByNameAndPassword(userModel.getName(), userModel.getPassword());
    }
}
