package com.nspay.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.*;

@Configuration
public class OpenApiConfiguration {
    @Bean public OpenAPI applicationApi() {
        return new OpenAPI().info(new Info().title("NSPay API").version("1.0")
            .description("Accounts, payments, savings, investments, documents and security reviews. "
                + "Sign in through POST /api/login/, copy the returned token, then use Authorize to set Bearer authentication. "
                + "Ownership is derived from the token. Administrator operations also check the user's stored role. "
                + "Restricted accounts retain read access but cannot move funds. Investment holdings are recorded in NSPay; no broker is connected."))
            .components(new Components().addSecuritySchemes("bearerAuth",new SecurityScheme()
                .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
    @Bean public OperationCustomizer documentedErrors() {
        return (operation,handler) -> {
            if (operation.getResponses()==null) operation.setResponses(new io.swagger.v3.oas.models.responses.ApiResponses());
            operation.getResponses().putIfAbsent("400",new ApiResponse().description("Invalid input or insufficient available funds; no financial change is committed."));
            operation.getResponses().putIfAbsent("401",new ApiResponse().description("Missing, expired or invalid session token, or invalid sign-in credentials."));
            if (!handler.getBeanType().getSimpleName().equals("LoginController")) {
                operation.getResponses().putIfAbsent("403",new ApiResponse().description("The authenticated user does not have access to this operation or resource."));
                operation.getResponses().putIfAbsent("404",new ApiResponse().description("The requested resource was not found or is not owned by this user."));
                operation.getResponses().putIfAbsent("409",new ApiResponse().description("State conflict, expired quote, conflicting request key, or a decision already recorded."));
            }
            return operation;
        };
    }
}
