package com.tahaberkamcadev.e_com.user_service.service.impl;

import com.tahaberkamcadev.e_com.user_service.dto.request.ChangePasswordRequest;
import com.tahaberkamcadev.e_com.user_service.dto.request.UpdateUserRequest;
import com.tahaberkamcadev.e_com.user_service.dto.request.UpdateUserPreferencesRequest;
import com.tahaberkamcadev.e_com.user_service.dto.response.UserResponse;
import com.tahaberkamcadev.e_com.user_service.exception.UnauthorizedAccessException;
import com.tahaberkamcadev.e_com.user_service.exception.UserNotFoundException;
import com.tahaberkamcadev.e_com.user_service.model.Role;
import com.tahaberkamcadev.e_com.user_service.model.User;
import com.tahaberkamcadev.e_com.user_service.repository.UserRepository;
import com.tahaberkamcadev.e_com.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implementation of UserService providing comprehensive user management functionality.
 * 
 * This service handles:
 * - User CRUD operations with proper authorization
 * - Profile management and preferences
 * - Password change functionality
 * - Role-based access control
 * 
 * @author Taha Berk Amcadeva
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        User authenticatedUser = getAuthenticatedUser();

        if (!authenticatedUser.getId().equals(id) && authenticatedUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedAccessException();
        }

        return userRepository.findById(id)
                .map(UserResponse::fromEntity)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        return UserResponse.fromEntity(getAuthenticatedUser());
    }

    @Override
    @Transactional
    public UserResponse updateCurrentUser(UpdateUserRequest request) {
        User authenticatedUser = getAuthenticatedUser();
        
        if (request.firstName() != null && !request.firstName().isBlank()) {
            authenticatedUser.setFirstName(request.firstName().trim());
        }
        if (request.lastName() != null && !request.lastName().isBlank()) {
            authenticatedUser.setLastName(request.lastName().trim());
        }

        User savedUser = userRepository.save(authenticatedUser);
        log.info("User updated their own profile: {}", savedUser.getId());

        return UserResponse.fromEntity(savedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        User authenticatedUser = getAuthenticatedUser();

        if (!authenticatedUser.getId().equals(id) && authenticatedUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedAccessException();
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        if (request.firstName() != null && !request.firstName().isBlank()) {
            user.setFirstName(request.firstName().trim());
        }
        if (request.lastName() != null && !request.lastName().isBlank()) {
            user.setLastName(request.lastName().trim());
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        User savedUser = userRepository.save(user);
        log.info("User updated with id: {}", savedUser.getId());

        return UserResponse.fromEntity(savedUser);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User authenticatedUser = getAuthenticatedUser();
        
        if (!passwordEncoder.matches(request.currentPassword(), authenticatedUser.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        
        authenticatedUser.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(authenticatedUser);
        
        log.info("User changed password: {}", authenticatedUser.getId());
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        
        userRepository.delete(user);
        log.info("User deleted with id: {}", id);
    }

    @Override
    @Transactional
    public UserResponse updateCurrentUserPreferences(UpdateUserPreferencesRequest request) {
        User authenticatedUser = getAuthenticatedUser();
        User user = userRepository.findById(authenticatedUser.getId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Update preferences if provided
        if (request.phoneNumber() != null) {
            user.setPhoneNumber(request.phoneNumber().trim());
        }
        if (request.preferredLanguage() != null) {
            user.setPreferredLanguage(request.preferredLanguage());
        }
        if (request.preferredCurrency() != null) {
            user.setPreferredCurrency(request.preferredCurrency().toUpperCase());
        }
        if (request.marketingConsent() != null) {
            user.setMarketingConsent(request.marketingConsent());
        }
        if (request.smsConsent() != null) {
            user.setSmsConsent(request.smsConsent());
        }
        if (request.emailConsent() != null) {
            user.setEmailConsent(request.emailConsent());
        }

        User updatedUser = userRepository.save(user);
        log.info("User preferences updated for user: {}", updatedUser.getId());
        
        return UserResponse.fromEntity(updatedUser);
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new UnauthorizedAccessException("Authentication context not found");
        }
        return user;
    }
}
