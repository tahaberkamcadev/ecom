package com.tahaberkamcadev.e_com.user_service.service.impl;

import com.tahaberkamcadev.e_com.user_service.dto.request.LoginRequest;
import com.tahaberkamcadev.e_com.user_service.dto.request.RegisterRequest;
import com.tahaberkamcadev.e_com.user_service.dto.request.VerifyEmailRequest;
import com.tahaberkamcadev.e_com.user_service.dto.response.AuthResponse;
import com.tahaberkamcadev.e_com.user_service.exception.EmailAlreadyExistsException;
import com.tahaberkamcadev.e_com.user_service.exception.UnauthorizedAccessException;
import com.tahaberkamcadev.e_com.user_service.exception.UserNotFoundException;
import com.tahaberkamcadev.e_com.user_service.model.Role;
import com.tahaberkamcadev.e_com.user_service.model.User;
import com.tahaberkamcadev.e_com.user_service.repository.UserRepository;
import com.tahaberkamcadev.e_com.user_service.security.JwtService;
import com.tahaberkamcadev.e_com.user_service.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
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
                .verificationToken(generateVerificationToken())
                .verificationTokenExpiresAt(LocalDateTime.now().plusHours(24))
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

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        final String normalizedEmail = normalizeEmail(request.email());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
        );

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UserNotFoundException(normalizedEmail));

        // Update login statistics
        user.setLastLoginAt(LocalDateTime.now());
        user.setLoginCount(user.getLoginCount() + 1);
        User updatedUser = userRepository.save(user);

        log.info("User logged in with id: {} (total logins: {})", updatedUser.getId(), updatedUser.getLoginCount());

        Map<String, Object> claims = Map.of(
                "userId", updatedUser.getId().toString(),
                "role", updatedUser.getRole().name()
        );
        String token = jwtService.generateToken(claims, updatedUser);
        return AuthResponse.of(token, jwtService.getExpirationTime());
    }

    @Override
    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        final String token = request.token().trim();
        
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new UserNotFoundException("Invalid or expired verification token"));

        if (user.getVerificationTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedAccessException("Verification token has expired");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiresAt(null);
        User verifiedUser = userRepository.save(user);

        log.info("Email verified for user: {}", verifiedUser.getId());
    }

    @Override
    @Transactional
    public void resendVerificationEmail(String email) {
        final String normalizedEmail = normalizeEmail(email);
        
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UserNotFoundException(normalizedEmail));

        if (user.isEmailVerified()) {
            throw new UnauthorizedAccessException("Email is already verified");
        }

        // Generate new verification token
        user.setVerificationToken(generateVerificationToken());
        user.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        log.info("Verification email resent for user: {}", user.getId());
        // In production: Send email via email service
    }

    private String normalizeEmail(String email) {
        return email.toLowerCase().trim();
    }

    private String generateVerificationToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
