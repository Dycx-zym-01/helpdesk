package com.example.helpdesk.controller;

import com.example.helpdesk.domain.Role;
import com.example.helpdesk.dto.UserModels;
import com.example.helpdesk.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserModels.UserResponse> listUsers(
            @RequestHeader(name = "X-USER-ID", required = false) Long userId,
            @RequestParam(required = false) Role role) {
        return userService.listUsers(userId, role);
    }

    @PostMapping
    public UserModels.UserResponse createUser(
            @RequestHeader(name = "X-USER-ID", required = false) Long userId,
            @Valid @RequestBody UserModels.CreateUserRequest request) {
        return userService.createUser(userId, request);
    }

    @PutMapping("/{id}")
    public UserModels.UserResponse updateUser(
            @RequestHeader(name = "X-USER-ID", required = false) Long userId,
            @PathVariable Long id,
            @Valid @RequestBody UserModels.UpdateUserRequest request) {
        return userService.updateUser(userId, id, request);
    }
}
