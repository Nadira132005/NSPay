package com.nspay.dto;

import com.nspay.enums.UserRole;
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
