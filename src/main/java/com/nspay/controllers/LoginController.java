package com.nspay.controllers;

import com.nspay.dto.LoginResponseDTO;
import com.nspay.entities.UserModel;
import com.nspay.services.AuditService;
import com.nspay.services.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@io.swagger.v3.oas.annotations.tags.Tag(name="Authentication")
@RestController
@RequestMapping(value = "/api/login")
public class LoginController {

    @Autowired
    UserService userService;

    @Autowired
    AuditService auditService;

    private static final Logger logger = LogManager.getLogger();

    @RequestMapping(method = RequestMethod.POST, path = "/", consumes = MediaType.APPLICATION_JSON_VALUE)
    @io.swagger.v3.oas.annotations.security.SecurityRequirements
    @io.swagger.v3.oas.annotations.Operation(summary="Sign in", description="Submit name and password. Returns a JWT token, role, user ID and passwordChangeRequired. When a password change is required, only PUT /api/users/me/password is permitted until it is completed.")
    public LoginResponseDTO loginUser(@javax.validation.Valid @RequestBody com.nspay.dto.LoginRequestDTO credentials) {
        UserModel userModel=new UserModel();userModel.setName(credentials.getName());userModel.setPassword(credentials.getPassword());
        logger.info("Processing login for {}", userModel.getName());
        LoginResponseDTO response = userService.loginUser(userModel);
        if (response == null) {
            String attemptedUser = userModel.getName();
            if (attemptedUser == null || attemptedUser.trim().isEmpty()) {
                attemptedUser = "UNKNOWN";
            }
            try {
                auditService.record(attemptedUser, "LOGIN_FAILED");
            } catch (Exception auditError) {
                logger.warn("Audit logging failed for login failure: {}", auditError.getMessage());
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        try {
            auditService.record(response.getName(), "LOGIN_SUCCESS");
        } catch (Exception auditError) {
            logger.warn("Audit logging failed for login success: {}", auditError.getMessage());
        }
        return response;
    }
}
