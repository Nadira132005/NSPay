package com.nspay.dto;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
@Data @Schema(name="SignInCredentials")
public class LoginRequestDTO {
    @NotBlank @Schema(example="alice") private String name;
    @NotBlank @Schema(format="password",description="Account password") private String password;
}
