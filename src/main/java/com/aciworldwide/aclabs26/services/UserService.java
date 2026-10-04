package com.aciworldwide.aclabs26.services;

import com.aciworldwide.aclabs26.dto.LoginResponseDTO;
import com.aciworldwide.aclabs26.entities.AccountModel;
import com.aciworldwide.aclabs26.entities.UserModel;
import com.aciworldwide.aclabs26.enums.UserRole;
import com.aciworldwide.aclabs26.repository.AccountRepository;
import com.aciworldwide.aclabs26.repository.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import javax.transaction.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@Log4j2
public class UserService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    JwtService jwtService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public LoginResponseDTO loginUser(UserModel userModel) {
        UserModel existingUser = userRepository.findByName(userModel.getName());
        log.info("Login user found: {}", existingUser.getName());
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

        String accountCardNumber = null;
        if (existingUser.getAccountId() != null) {
            AccountModel account = accountRepository.findById(existingUser.getAccountId()).orElse(null);
            accountCardNumber = account == null ? null : account.getCardNumber();
        }

        String token = jwtService.generateToken(existingUser, accountCardNumber);
        UserRole role = existingUser.getRole() == null ? UserRole.User : existingUser.getRole();
        return new LoginResponseDTO(token, existingUser.getId(), role, existingUser.getName(), existingUser.isPasswordChangeRequired());
    }
    @Transactional
    public UserModel createUser(UserModel userModel) {
        if (userModel.getName() != null) userModel.setName(userModel.getName().trim());
        if (userModel.getFirstName() != null) userModel.setFirstName(userModel.getFirstName().trim());
        if (userModel.getLastName() != null) userModel.setLastName(userModel.getLastName().trim());
        if (userModel.getName() == null || userModel.getName().trim().isEmpty()
                || userModel.getFirstName() == null || userModel.getFirstName().trim().isEmpty()
                || userModel.getLastName() == null || userModel.getLastName().trim().isEmpty()
                || userModel.getAccountId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username, first name, last name and account are required");
        }
        if (userRepository.existsByName(userModel.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User name already exists");
        }
        applyAccount(userModel.getAccountId());
        ensureAccountNotAssigned(userModel.getAccountId(), null);
        userModel.setPassword(passwordEncoder.encode(userModel.getName()));
        userModel.setPasswordChangeRequired(true);
        return userRepository.save(userModel);
    }

    @Transactional
    public UserModel updateUser(long id, UserModel userModel) {
        UserModel existingUser = userRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User with id " + id + " is not found"));
        if (userModel.getAccountId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each user must have an account and IBAN");
        }
        applyAccount(userModel.getAccountId());
        ensureAccountNotAssigned(userModel.getAccountId(), id);
        existingUser.setAccountId(userModel.getAccountId());
        existingUser.setFirstName(userModel.getFirstName());
        existingUser.setLastName(userModel.getLastName());
        return userRepository.save(existingUser);
    }

    @Transactional
    public LoginResponseDTO changePassword(long userId, String newPassword) {
        if (newPassword == null || newPassword.trim().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must contain at least 8 characters");
        }
        UserModel user = userRepository.findById(userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Signed-in user was not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangeRequired(false);
        userRepository.save(user);
        String cardNumber = null;
        if (user.getAccountId() != null) {
            AccountModel account = accountRepository.findById(user.getAccountId()).orElse(null);
            cardNumber = account == null ? null : account.getCardNumber();
        }
        UserRole role = user.getRole() == null ? UserRole.User : user.getRole();
        return new LoginResponseDTO(jwtService.generateToken(user, cardNumber), user.getId(), role,
                user.getName(), false);
    }

    @Transactional
    public List<UserModel> getAllUsers() {
        List<UserModel> users = new ArrayList<>();
        userRepository.findAll().forEach(users::add);
        return users;
    }

    @Transactional
    public UserModel getCurrentUser(String name) {
        UserModel currentUser = userRepository.findByName(name);
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Current user is not found");
        }
        return currentUser;
    }

    @Transactional
    public void deleteUser(long id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User with id " + id + " is not found");
        }
        userRepository.deleteById(id);
    }

    private void applyAccount(Long accountId) {
        if (accountId == null) {
            return;
        }
        if (!accountRepository.existsById(accountId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account with id " + accountId + " is not found");
        }
    }

    private void ensureAccountNotAssigned(Long accountId, Long exceptUserId) {
        if (accountId == null) return;
        boolean alreadyAssigned = exceptUserId == null
                ? userRepository.existsByAccountId(accountId)
                : userRepository.existsByAccountIdAndIdNot(accountId, exceptUserId);
        if (alreadyAssigned) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Each user must have a separate account and IBAN");
        }
    }
}
