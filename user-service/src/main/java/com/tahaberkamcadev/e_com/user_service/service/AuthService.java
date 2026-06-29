package com.tahaberkamcadev.e_com.user_service.service;

import com.tahaberkamcadev.e_com.user_service.dto.request.LoginRequest;
import com.tahaberkamcadev.e_com.user_service.dto.request.RegisterRequest;
import com.tahaberkamcadev.e_com.user_service.dto.response.AuthResponse;
import com.tahaberkamcadev.e_com.user_service.exception.EmailAlreadyExistsException;
import com.tahaberkamcadev.e_com.user_service.exception.UserNotFoundException;
import com.tahaberkamcadev.e_com.user_service.model.Role;
import com.tahaberkamcadev.e_com.user_service.model.User;
import com.tahaberkamcadev.e_com.user_service.repository.UserRepository;
import com.tahaberkamcadev.e_com.user_service.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        final String normalizedEmail = normalizeEmail(request.email());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        User user = User.builder()
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.password()))
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("New user registered with id: {}", savedUser.getId());

        Map<String, Object> claims = Map.of(
                "userId", savedUser.getId().toString(),
                "role", savedUser.getRole().name()
        );
        String token = jwtService.generateToken(claims, savedUser);
        return AuthResponse.of(token, jwtService.getExpirationTime());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        final String normalizedEmail = normalizeEmail(request.email());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
        );

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UserNotFoundException(normalizedEmail));

        log.info("User logged in with id: {}", user.getId());

        Map<String, Object> claims = Map.of(
                "userId", user.getId().toString(),
                "role", user.getRole().name()
        );
        String token = jwtService.generateToken(claims, user);
        return AuthResponse.of(token, jwtService.getExpirationTime());
    }

    private String normalizeEmail(String email) {
        return email.toLowerCase().trim();
    }
}
