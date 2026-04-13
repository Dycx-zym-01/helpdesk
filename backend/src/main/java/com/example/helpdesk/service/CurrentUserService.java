package com.example.helpdesk.service;

import com.example.helpdesk.domain.Role;
import com.example.helpdesk.domain.User;
import com.example.helpdesk.exception.BusinessException;
import com.example.helpdesk.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User requireCurrentUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "登录用户不存在"));
        if (!user.isEnabled()) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "当前账号已被禁用");
        }
        return user;
    }

    public void requireRole(User user, Role... roles) {
        boolean matched = Arrays.stream(roles).anyMatch(role -> role == user.getRole());
        if (!matched) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "当前角色没有该操作权限");
        }
    }
}
