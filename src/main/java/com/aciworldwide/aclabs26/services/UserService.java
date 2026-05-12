package com.aciworldwide.aclabs26.services;

import com.aciworldwide.aclabs26.dto.LoginResponseDTO;
import com.aciworldwide.aclabs26.entities.UserModel;
import com.aciworldwide.aclabs26.enums.UserRole;
import com.aciworldwide.aclabs26.repository.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import javax.transaction.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Log4j2
public class UserService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    JwtService jwtService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public LoginResponseDTO loginUser(UserModel userModel) {
        UserModel existingUser = userRepository.findByName(userModel.getName());
        log.info(existingUser);
        if (existingUser == null) {
            return null;
        }

        String storedPassword = existingUser.getPassword();
        boolean passwordMatches = passwordEncoder.matches(userModel.getPassword(), storedPassword) || storedPassword.equals(userModel.getPassword());
        if (!passwordMatches) {
            return null;
        }

        if (storedPassword.equals(userModel.getPassword())) {
            existingUser.setPassword(passwordEncoder.encode(userModel.getPassword()));
            userRepository.save(existingUser);
        }

        String token = jwtService.generateToken(existingUser);
        UserRole role = existingUser.getRole() == null ? UserRole.User : existingUser.getRole();
        return new LoginResponseDTO(token, existingUser.getId(), role, existingUser.getName());
    }
    @Transactional
    public UserModel createUser(UserModel userModel) {
        if (userRepository.existsByName(userModel.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User name already exists");
        }
        userModel.setPassword(passwordEncoder.encode(userModel.getPassword()));
        return userRepository.save(userModel);
    }
}
