package com.nikhil.productapi.config;

import com.nikhil.productapi.entity.AppUser;
import com.nikhil.productapi.entity.Role;
import com.nikhil.productapi.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-username}")
    private String adminUsername;
    @Value("${app.seed.admin-password}")
    private String adminPassword;
    @Value("${app.seed.user-username}")
    private String userUsername;
    @Value("${app.seed.user-password}")
    private String userPassword;

    @Override
    public void run(String... args) {
        seed(adminUsername, adminPassword, Role.ROLE_ADMIN);
        seed(userUsername, userPassword, Role.ROLE_USER);
    }

    private void seed(String username, String rawPassword, Role role) {
        if (appUserRepository.findByUsername(username).isEmpty()) {
            appUserRepository.save(AppUser.builder()
                    .username(username)
                    .password(passwordEncoder.encode(rawPassword))
                    .role(role)
                    .build());
        }
    }
}
