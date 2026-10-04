package com.aciworldwide.aclabs26.dto;

import com.aciworldwide.aclabs26.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponseDTO {
    private String token;
    private Long userId;
    private UserRole role;
    private String name;
    private boolean passwordChangeRequired;
}
