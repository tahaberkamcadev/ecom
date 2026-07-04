package com.tahaberkamcadev.e_com.user_service.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.tahaberkamcadev.e_com.user_service.entity.Role;
import com.tahaberkamcadev.e_com.user_service.entity.User;
import com.tahaberkamcadev.e_com.user_service.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DevDataSeeder implements ApplicationRunner {

    static final String ADMIN_EMAIL = "admin@demo.local";
    static final String ADMIN_PASSWORD = "DemoAdmin1!";
    static final String CUSTOMER_EMAIL = "customer@demo.local";
    static final String CUSTOMER_PASSWORD = "DemoCustomer1!";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        seedUser(ADMIN_EMAIL, "Demo", "Admin", ADMIN_PASSWORD, Role.ADMIN);
        seedUser(CUSTOMER_EMAIL, "Demo", "Customer", CUSTOMER_PASSWORD, Role.CUSTOMER);
    }

    private void seedUser(String email, String firstName, String lastName, String rawPassword, Role role) {
        if (userRepository.existsByEmail(email)) {
            return;
        }

        User user = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .enabled(true)
                .build();

        userRepository.save(user);
        log.info("Seeded {} user: {}", role.name().toLowerCase(), email);
    }
}
