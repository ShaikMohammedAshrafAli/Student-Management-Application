package com.example.authservice.service;

import com.example.authservice.dto.UserResponse;
import com.example.authservice.entity.Role;
import com.example.authservice.entity.User;
import com.example.authservice.repository.UserRepository;
import com.example.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Admin-only operations on user accounts: listing, role assignment,
 * and activation/deactivation. Kept separate from AuthService, which
 * only concerns itself with the authentication flow itself.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserManagementService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(UserResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return UserResponse.fromEntity(findUserOrThrow(id));
    }

    public UserResponse assignRole(Long id, String role) {
        User user = findUserOrThrow(id);
        user.setRole(Role.valueOf(role.toUpperCase()));
        return UserResponse.fromEntity(userRepository.save(user));
    }

    public UserResponse setEnabled(Long id, boolean enabled) {
        User user = findUserOrThrow(id);
        user.setEnabled(enabled);
        return UserResponse.fromEntity(userRepository.save(user));
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }
}
