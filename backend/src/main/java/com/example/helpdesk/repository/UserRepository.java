package com.example.helpdesk.repository;

import com.example.helpdesk.domain.Role;
import com.example.helpdesk.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    List<User> findByRole(Role role);

    List<User> findByRoleIn(Collection<Role> roles);
}
