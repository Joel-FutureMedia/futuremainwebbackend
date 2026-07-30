package com.futuremedia.config;

import com.futuremedia.models.Role;
import com.futuremedia.models.User;
import com.futuremedia.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.super-admin.email}")
    private String superAdminEmail;

    @Value("${app.super-admin.password}")
    private String superAdminPassword;

    @Value("${app.super-admin.full-name}")
    private String superAdminFullName;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail(superAdminEmail)) {
            User superAdmin = new User();
            superAdmin.setFullName(superAdminFullName);
            superAdmin.setEmail(superAdminEmail);
            superAdmin.setPassword(passwordEncoder.encode(superAdminPassword));
            superAdmin.setRole(Role.SUPER_ADMIN);
            superAdmin.setEnabled(true);
            userRepository.save(superAdmin);
            System.out.println("Default Super Admin created: " + superAdminEmail);
        } else {
            System.out.println("Super Admin already exists: " + superAdminEmail);
        }
    }
}
