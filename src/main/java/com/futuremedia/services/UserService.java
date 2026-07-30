package com.futuremedia.services;

import com.futuremedia.models.BadRequestException;
import com.futuremedia.models.ResourceNotFoundException;
import com.futuremedia.models.Role;
import com.futuremedia.models.User;
import com.futuremedia.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class UserService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.super-admin.email}")
    private String defaultSuperAdminEmail;

    public User createUser(String fullName, String email, String password, Role role) {
        validateUserInput(fullName, email, password);

        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BadRequestException("Email already exists");
        }

        if (role == null) {
            throw new BadRequestException("Role is required. Use SUPER_ADMIN or USER");
        }

        User user = new User();
        user.setFullName(fullName.trim());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    public User updateUser(Long id, String fullName, String email, String password, Role role, Boolean enabled) {
        User user = getById(id);

        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName.trim());
        }

        if (email != null && !email.isBlank()) {
            String normalizedEmail = email.trim().toLowerCase();
            if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
                throw new BadRequestException("Invalid email format");
            }
            if (!normalizedEmail.equals(user.getEmail()) && userRepository.existsByEmail(normalizedEmail)) {
                throw new BadRequestException("Email already exists");
            }
            user.setEmail(normalizedEmail);
        }

        if (password != null && !password.isBlank()) {
            if (password.length() < 6) {
                throw new BadRequestException("Password must be at least 6 characters");
            }
            user.setPassword(passwordEncoder.encode(password));
        }

        if (role != null) {
            user.setRole(role);
        }

        if (enabled != null) {
            if (isDefaultSuperAdmin(user) && !enabled) {
                throw new BadRequestException("Cannot disable the default Super Admin account");
            }
            user.setEnabled(enabled);
        }

        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        User user = getById(id);
        if (isDefaultSuperAdmin(user)) {
            throw new BadRequestException("Cannot delete the default Super Admin account");
        }
        userRepository.delete(user);
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> searchUsers(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllUsers();
        }
        return userRepository.searchUsers(keyword.trim());
    }

    public User enableUser(Long id) {
        User user = getById(id);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    public User disableUser(Long id) {
        User user = getById(id);
        if (isDefaultSuperAdmin(user)) {
            throw new BadRequestException("Cannot disable the default Super Admin account");
        }
        user.setEnabled(false);
        return userRepository.save(user);
    }

    private boolean isDefaultSuperAdmin(User user) {
        return user.getEmail().equalsIgnoreCase(defaultSuperAdminEmail);
    }

    private void validateUserInput(String fullName, String email, String password) {
        if (fullName == null || fullName.isBlank()) {
            throw new BadRequestException("Full name is required");
        }
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email is required");
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new BadRequestException("Invalid email format");
        }
        if (password == null || password.isBlank()) {
            throw new BadRequestException("Password is required");
        }
        if (password.length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters");
        }
    }
}
