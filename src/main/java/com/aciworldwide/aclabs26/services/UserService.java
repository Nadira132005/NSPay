package com.aciworldwide.aclabs26.services;

import com.aciworldwide.aclabs26.dto.LoginResponseDTO;
import com.aciworldwide.aclabs26.entities.UserModel;
import com.aciworldwide.aclabs26.enums.UserRole;
import com.aciworldwide.aclabs26.repository.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import javax.transaction.Transactional;
import java.util.UUID;
import org.springframework.web.server.ResponseStatusException;

@Service
@Log4j2
public class UserService {

    @Autowired
    UserRepository userRepository;

    @Transactional
    public LoginResponseDTO loginUser(UserModel userModel) {
        UserModel existingUser = userRepository.findUserModelByNameAndPassword(userModel.getName(), userModel.getPassword());
        log.info(existingUser);
        if (existingUser == null) {
            return null;
        }
        String token = UUID.randomUUID().toString();
        UserRole role = existingUser.getRole() == null ? UserRole.User : existingUser.getRole();
        return new LoginResponseDTO(token, existingUser.getId(), role, existingUser.getName());
    }
    @Transactional
    public UserModel createUser(UserModel userModel) {
        if (userRepository.existsByName(userModel.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User name already exists");
        }
        return userRepository.save(userModel);
    }
}
