package com.example.helpdesk.dto;

import com.example.helpdesk.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class UserModels {

    private UserModels() {
    }

    public record UserResponse(
            Long id,
            String username,
            String name,
            String email,
            Role role,
            String roleLabel,
            boolean enabled
    ) {
    }

    public record CreateUserRequest(
            @NotBlank(message = "用户名不能为空") String username,
            @NotBlank(message = "姓名不能为空") String name,
            @NotBlank(message = "密码不能为空") String password,
            @Email(message = "邮箱格式不正确") String email,
            @NotNull(message = "角色不能为空") Role role
    ) {
    }

    public record UpdateUserRequest(
            @NotBlank(message = "姓名不能为空") String name,
            @Email(message = "邮箱格式不正确") String email,
            @NotNull(message = "角色不能为空") Role role,
            boolean enabled,
            String password
    ) {
    }
}
