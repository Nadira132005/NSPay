package com.nspay.controllers;

import com.nspay.entities.UserModel;
import com.nspay.dto.ChangePasswordDTO;
import com.nspay.dto.LoginResponseDTO;
import com.nspay.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import javax.servlet.http.HttpServletRequest; //validated JWT, not from a header that client could fake

import java.util.List;

@io.swagger.v3.oas.annotations.tags.Tag(name="Users")
@RestController
@RequestMapping(value = "/api/users")
public class UserController {

    @Autowired
    UserService userService;

    @RequestMapping(method = RequestMethod.POST, path = "/", consumes = MediaType.APPLICATION_JSON_VALUE)
    @io.swagger.v3.oas.annotations.Operation(summary="Create a user", description="Administrator only. Requires a unique username, name and an unassigned account. Temporary password is the username; it must be changed on first sign-in.")
    public UserModel createUser(
            @RequestBody UserModel userModel,
            HttpServletRequest request //access to current HTTP request context
    ) {
        assertAdmin(request);
        return userService.createUser(userModel);
    }

    @GetMapping({"", "/"})
    @io.swagger.v3.oas.annotations.Operation(summary="List users", description="Administrator or analyst only. Passwords are excluded from responses.")
    public ResponseEntity<List<UserModel>> getUsers(HttpServletRequest request) {
        assertReadOnly(request);
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/me")
    @io.swagger.v3.oas.annotations.Operation(summary="Get the signed-in user", description="Returns the current user from the verified session identity.")
    public ResponseEntity<UserModel> getCurrentUser(HttpServletRequest request) {
        String currentName = (String) request.getAttribute("authenticatedName");
        if (currentName == null || currentName.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current user is not available");
        }
        return ResponseEntity.ok(userService.getCurrentUser(currentName));
    }

    @PutMapping("/me/password")
    @io.swagger.v3.oas.annotations.Operation(summary="Change the current user’s password", description="Password must contain at least eight characters. Clears the first-login password requirement and returns a renewed token.")
    public ResponseEntity<LoginResponseDTO> changeOwnPassword(@RequestBody ChangePasswordDTO body,
                                                               HttpServletRequest request) {
        Number userId = (Number) request.getAttribute("authenticatedUserId");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current user is not available");
        }
        return ResponseEntity.ok(userService.changePassword(userId.longValue(), body.getPassword()));
    }

    @DeleteMapping("/{id}")
    @io.swagger.v3.oas.annotations.Operation(summary="Delete a user", description="Administrator only.")
    public ResponseEntity<Void> deleteUser(@PathVariable long id, HttpServletRequest request) {
        assertAdmin(request);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    @io.swagger.v3.oas.annotations.Operation(summary="Update user details", description="Administrator only. Updates name fields. An already assigned account cannot be replaced to bypass its restrictions.")
    public UserModel updateUser(
            @PathVariable long id,
            @RequestBody UserModel userModel,
            HttpServletRequest request
    ) {
        assertAdmin(request);
        return userService.updateUser(id, userModel);
    }

    private void assertAdmin(HttpServletRequest request) {
        String currentRole = (String) request.getAttribute("authenticatedRole");
        if (!"Admin".equals(currentRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Admin can manage users");
        }
    }

    private void assertReadOnly(HttpServletRequest request) {
        String currentRole = (String) request.getAttribute("authenticatedRole");
        if (!"Admin".equals(currentRole) && !"Analyst".equals(currentRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Admin or Analyst can view users");
        }
    }
}
