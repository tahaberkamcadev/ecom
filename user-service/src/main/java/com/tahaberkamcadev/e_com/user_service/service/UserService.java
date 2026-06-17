package com.tahaberkamcadev.e_com.user_service.service;

import com.tahaberkamcadev.e_com.user_service.dto.request.ChangePasswordRequest;
import com.tahaberkamcadev.e_com.user_service.dto.request.UpdateUserRequest;
import com.tahaberkamcadev.e_com.user_service.dto.response.UserResponse;
import com.tahaberkamcadev.e_com.user_service.exception.UnauthorizedAccessException;
import com.tahaberkamcadev.e_com.user_service.exception.UserNotFoundException;
import com.tahaberkamcadev.e_com.user_service.model.Role;
import com.tahaberkamcadev.e_com.user_service.model.User;
import com.tahaberkamcadev.e_com.user_service.repository.UserRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OutboxEventService outboxEventService;

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

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

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        return UserResponse.fromEntity(getAuthenticatedUser());
    }

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

    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        UUID userId = user.getId();
        String email = user.getEmail();

        userRepository.delete(user);
        outboxEventService.saveUserDeletedEvent(userId, email);
        log.info("User deleted with id: {}", id);
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new UnauthorizedAccessException("Authentication context not found");
        }
        return user;
    }
}
