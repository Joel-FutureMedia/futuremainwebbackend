package com.futuremedia.controllers;

import com.futuremedia.models.ApiResponse;
import com.futuremedia.models.Role;
import com.futuremedia.models.User;
import com.futuremedia.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createUser(@RequestBody Map<String, String> body) {
        Role role;
        try {
            role = Role.valueOf(body.get("role").trim().toUpperCase());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("Invalid role. Use SUPER_ADMIN or USER"));
        }

        User user = userService.createUser(
                body.get("fullName"),
                body.get("email"),
                body.get("password"),
                role
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("User created successfully", toSafeMap(user)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllUsers() {
        List<Map<String, Object>> users = userService.getAllUsers().stream()
                .map(this::toSafeMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Users retrieved successfully", users));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> searchUsers(@RequestParam String keyword) {
        List<Map<String, Object>> users = userService.searchUsers(keyword).stream()
                .map(this::toSafeMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Search results", users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("User retrieved successfully", toSafeMap(userService.getById(id))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateUser(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        Role role = null;
        if (body.get("role") != null) {
            role = Role.valueOf(body.get("role").toString().trim().toUpperCase());
        }
        Boolean enabled = body.get("enabled") != null ? Boolean.valueOf(body.get("enabled").toString()) : null;

        User user = userService.updateUser(
                id,
                body.get("fullName") != null ? body.get("fullName").toString() : null,
                body.get("email") != null ? body.get("email").toString() : null,
                body.get("password") != null ? body.get("password").toString() : null,
                role,
                enabled
        );
        return ResponseEntity.ok(ApiResponse.ok("User updated successfully", toSafeMap(user)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.ok("User deleted successfully"));
    }

    @PutMapping("/{id}/enable")
    public ResponseEntity<ApiResponse<Map<String, Object>>> enableUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("User enabled", toSafeMap(userService.enableUser(id))));
    }

    @PutMapping("/{id}/disable")
    public ResponseEntity<ApiResponse<Map<String, Object>>> disableUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("User disabled", toSafeMap(userService.disableUser(id))));
    }

    private Map<String, Object> toSafeMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("fullName", user.getFullName());
        map.put("email", user.getEmail());
        map.put("role", user.getRole().name());
        map.put("enabled", user.isEnabled());
        map.put("createdAt", user.getCreatedAt());
        map.put("updatedAt", user.getUpdatedAt());
        return map;
    }
}
