package com.example.helpdesk.dto;

import jakarta.validation.constraints.NotBlank;

public final class AuthModels {

    private AuthModels() {
    }

    public record LoginRequest(
            @NotBlank(message = "用户名不能为空") String username,
            @NotBlank(message = "密码不能为空") String password
    ) {
    }

    public record AuthResponse(
            String token,
            UserModels.UserResponse user
    ) {
    }
}
