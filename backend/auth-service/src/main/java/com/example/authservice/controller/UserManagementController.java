package com.example.authservice.controller;

import com.example.authservice.dto.UserResponse;
import com.example.authservice.service.UserManagementService;
import com.example.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ADMIN-only endpoints for managing user accounts: listing, role
 * assignment, and activation/deactivation.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "User Management", description = "Admin-only: manage user accounts and roles")
public class UserManagementController {

    private final UserManagementService userManagementService;

    @GetMapping
    @Operation(summary = "List all users")
    public ApiResponse<List<UserResponse>> getAllUsers() {
        return ApiResponse.success(userManagementService.getAllUsers());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single user by id")
    public ApiResponse<UserResponse> getUserById(@PathVariable Long id) {
        return ApiResponse.success(userManagementService.getUserById(id));
    }

    @PatchMapping("/{id}/role")
    @Operation(summary = "Assign a role (ADMIN or STUDENT) to a user")
    public ApiResponse<UserResponse> assignRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ApiResponse.success("Role updated", userManagementService.assignRole(id, body.get("role")));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activate or deactivate a user account")
    public ApiResponse<UserResponse> setEnabled(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        return ApiResponse.success("Status updated", userManagementService.setEnabled(id, body.get("enabled")));
    }
}
