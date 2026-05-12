package com.aciworldwide.aclabs26.controllers;

import com.aciworldwide.aclabs26.entities.UserModel;
import com.aciworldwide.aclabs26.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import javax.servlet.http.HttpServletRequest; //validated JWT, not from a header that client could fake

@RestController
@RequestMapping(value = "/api/users")
public class UserController {

    @Autowired
    UserService userService;

    @RequestMapping(method = RequestMethod.POST, path = "/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public UserModel createUser(
            @RequestBody UserModel userModel,
            HttpServletRequest request //access to current HTTP request context
    ) {
        String currentRole = (String) request.getAttribute("authenticatedRole");
        if (!"Admin".equals(currentRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Admin can create users");
        }
        return userService.createUser(userModel);
    }
}
