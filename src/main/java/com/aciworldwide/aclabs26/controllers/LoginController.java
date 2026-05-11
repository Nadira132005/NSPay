package com.aciworldwide.aclabs26.controllers;

import com.aciworldwide.aclabs26.dto.LoginResponseDTO;
import com.aciworldwide.aclabs26.entities.UserModel;
import com.aciworldwide.aclabs26.services.UserService;
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

@RestController
@RequestMapping(value = "/api/login")
public class LoginController {

    @Autowired
    UserService userService;

    private static final Logger logger = LogManager.getLogger();

    @RequestMapping(method = RequestMethod.POST, path = "/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LoginResponseDTO loginUser(@RequestBody() UserModel userModel) {
        logger.info("Processing login: {} with pwd {}", userModel.getName(), userModel.getPassword());
        LoginResponseDTO response = userService.loginUser(userModel);
        if (response == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return response;
    }
}
