package com.example.helpdesk.service;

import com.example.helpdesk.domain.Role;
import com.example.helpdesk.domain.User;
import com.example.helpdesk.dto.UserModels;
import com.example.helpdesk.exception.BusinessException;
import com.example.helpdesk.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public UserService(UserRepository userRepository, CurrentUserService currentUserService) {
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<UserModels.UserResponse> listUsers(Long currentUserId, Role role) {
        currentUserService.requireCurrentUser(currentUserId);
        List<User> users = role == null ? userRepository.findAll() : userRepository.findByRole(role);
        return users.stream()
                .sorted(Comparator.comparing(User::getId))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UserModels.UserResponse createUser(Long currentUserId, UserModels.CreateUserRequest request) {
        User operator = currentUserService.requireCurrentUser(currentUserId);
        currentUserService.requireRole(operator, Role.ADMIN);
        userRepository.findByUsername(request.username()).ifPresent(user -> {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "用户名已存在");
        });
        User user = new User();
        user.setUsername(request.username());
        user.setName(request.name());
        user.setPassword(request.password());
        user.setEmail(request.email());
        user.setRole(request.role());
        user.setEnabled(true);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserModels.UserResponse updateUser(Long currentUserId, Long id, UserModels.UpdateUserRequest request) {
        User operator = currentUserService.requireCurrentUser(currentUserId);
        currentUserService.requireRole(operator, Role.ADMIN);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));
        user.setName(request.name());
        user.setEmail(request.email());
        user.setRole(request.role());
        user.setEnabled(request.enabled());
        if (StringUtils.hasText(request.password())) {
            user.setPassword(request.password());
        }
        return toResponse(userRepository.save(user));
    }

    public UserModels.UserResponse toResponse(User user) {
        return new UserModels.UserResponse(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getRole().getLabel(),
                user.isEnabled()
        );
    }
}
