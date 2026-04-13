package com.example.helpdesk.service;

import com.example.helpdesk.domain.User;
import com.example.helpdesk.dto.AuthModels;
import com.example.helpdesk.exception.BusinessException;
import com.example.helpdesk.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final CurrentUserService currentUserService;

    public AuthService(UserRepository userRepository, UserService userService, CurrentUserService currentUserService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public AuthModels.AuthResponse login(AuthModels.LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "用户名或密码错误"));
        if (!user.isEnabled() || !user.getPassword().equals(request.password())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "用户名或密码错误");
        }
        return new AuthModels.AuthResponse(String.valueOf(user.getId()), userService.toResponse(user));
    }

    @Transactional(readOnly = true)
    public AuthModels.AuthResponse me(Long userId) {
        User user = currentUserService.requireCurrentUser(userId);
        return new AuthModels.AuthResponse(String.valueOf(user.getId()), userService.toResponse(user));
    }
}
