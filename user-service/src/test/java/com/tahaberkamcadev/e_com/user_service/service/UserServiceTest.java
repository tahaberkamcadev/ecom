package com.tahaberkamcadev.e_com.user_service.service;

import com.tahaberkamcadev.e_com.user_service.dto.request.UpdateUserRequest;
import com.tahaberkamcadev.e_com.user_service.dto.response.UserResponse;
import com.tahaberkamcadev.e_com.user_service.exception.UnauthorizedAccessException;
import com.tahaberkamcadev.e_com.user_service.exception.UserNotFoundException;
import com.tahaberkamcadev.e_com.user_service.entity.Role;
import com.tahaberkamcadev.e_com.user_service.entity.User;
import com.tahaberkamcadev.e_com.user_service.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User customerUser;
    private User adminUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        customerUser = User.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("encoded")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();

        adminUser = User.builder()
                .id(UUID.randomUUID())
                .firstName("Admin")
                .lastName("User")
                .email("admin@example.com")
                .password("encoded")
                .role(Role.ADMIN)
                .enabled(true)
                .build();

        otherUser = User.builder()
                .id(UUID.randomUUID())
                .firstName("Other")
                .lastName("User")
                .email("other@example.com")
                .password("encoded")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }

    @Test
    @DisplayName("getAllUsers: should return all users")
    void getAllUsers_ShouldReturnAllUsers() {
        given(userRepository.findAll()).willReturn(List.of(customerUser, adminUser));

        List<UserResponse> result = userService.getAllUsers();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).email()).isEqualTo("john@example.com");
        assertThat(result.get(1).email()).isEqualTo("admin@example.com");
    }

    @Test
    @DisplayName("getAllUsers: should return empty list when no users exist")
    void getAllUsers_WhenEmpty_ShouldReturnEmptyList() {
        given(userRepository.findAll()).willReturn(List.of());

        assertThat(userService.getAllUsers()).isEmpty();
    }

    @Test
    @DisplayName("getUserById: owner should be able to view their own profile")
    void getUserById_AsOwner_ShouldReturnUser() {
        authenticateAs(customerUser);
        given(userRepository.findById(customerUser.getId())).willReturn(Optional.of(customerUser));

        UserResponse result = userService.getUserById(customerUser.getId());

        assertThat(result.id()).isEqualTo(customerUser.getId());
        assertThat(result.email()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("getUserById: ADMIN should be able to view any user's profile")
    void getUserById_AsAdmin_ShouldReturnAnyUser() {
        authenticateAs(adminUser);
        given(userRepository.findById(customerUser.getId())).willReturn(Optional.of(customerUser));

        assertThat(userService.getUserById(customerUser.getId()).id()).isEqualTo(customerUser.getId());
    }

    @Test
    @DisplayName("getUserById: accessing another user's profile should throw UnauthorizedAccessException")
    void getUserById_AsOtherCustomer_ShouldThrowUnauthorizedException() {
        authenticateAs(customerUser);

        assertThatThrownBy(() -> userService.getUserById(otherUser.getId()))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    @DisplayName("getUserById: non-existent ID should throw UserNotFoundException")
    void getUserById_WithNonExistentId_ShouldThrowNotFoundException() {
        authenticateAs(adminUser);
        UUID randomId = UUID.randomUUID();
        given(userRepository.findById(randomId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(randomId))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("getCurrentUser: should return the authenticated user from SecurityContext")
    void getCurrentUser_ShouldReturnAuthenticatedUser() {
        authenticateAs(customerUser);

        UserResponse result = userService.getCurrentUser();

        assertThat(result.email()).isEqualTo(customerUser.getEmail());
        assertThat(result.firstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("updateUser: owner should be able to update their own profile")
    void updateUser_AsOwner_ShouldUpdateSuccessfully() {
        authenticateAs(customerUser);
        UpdateUserRequest request = new UpdateUserRequest("NewFirst", "NewLast", null);

        User updatedUser = User.builder()
                .id(customerUser.getId())
                .firstName("NewFirst")
                .lastName("NewLast")
                .email(customerUser.getEmail())
                .password(customerUser.getPassword())
                .role(customerUser.getRole())
                .enabled(true)
                .build();

        given(userRepository.findById(customerUser.getId())).willReturn(Optional.of(customerUser));
        given(userRepository.save(any(User.class))).willReturn(updatedUser);

        UserResponse result = userService.updateUser(customerUser.getId(), request);

        assertThat(result.firstName()).isEqualTo("NewFirst");
        assertThat(result.lastName()).isEqualTo("NewLast");
    }

    @Test
    @DisplayName("updateUser: new password should be encoded before saving")
    void updateUser_WithNewPassword_ShouldEncodePassword() {
        authenticateAs(customerUser);
        UpdateUserRequest request = new UpdateUserRequest(null, null, "newPassword123");

        given(userRepository.findById(customerUser.getId())).willReturn(Optional.of(customerUser));
        given(passwordEncoder.encode("newPassword123")).willReturn("$2a$10$newEncoded");
        given(userRepository.save(any(User.class))).willReturn(customerUser);

        userService.updateUser(customerUser.getId(), request);

        verify(passwordEncoder).encode("newPassword123");
    }

    @Test
    @DisplayName("updateUser: updating another user's profile should throw UnauthorizedAccessException")
    void updateUser_AsOtherCustomer_ShouldThrowUnauthorizedException() {
        authenticateAs(customerUser);

        assertThatThrownBy(() -> userService.updateUser(otherUser.getId(), new UpdateUserRequest("Name", null, null)))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    @DisplayName("updateUser: ADMIN should be able to update any user")
    void updateUser_AsAdmin_ShouldUpdateAnyUser() {
        authenticateAs(adminUser);
        UpdateUserRequest request = new UpdateUserRequest("NewFirst", null, null);

        User updated = User.builder()
                .id(customerUser.getId())
                .firstName("NewFirst")
                .lastName(customerUser.getLastName())
                .email(customerUser.getEmail())
                .password(customerUser.getPassword())
                .role(customerUser.getRole())
                .enabled(true)
                .build();

        given(userRepository.findById(customerUser.getId())).willReturn(Optional.of(customerUser));
        given(userRepository.save(any(User.class))).willReturn(updated);

        assertThat(userService.updateUser(customerUser.getId(), request).firstName()).isEqualTo("NewFirst");
    }

    @Test
    @DisplayName("deleteUser: existing user should be deleted successfully")
    void deleteUser_WithExistingUser_ShouldDelete() {
        given(userRepository.findById(customerUser.getId())).willReturn(Optional.of(customerUser));

        userService.deleteUser(customerUser.getId());

        verify(userRepository).delete(customerUser);
    }

    @Test
    @DisplayName("deleteUser: non-existent ID should throw UserNotFoundException")
    void deleteUser_WithNonExistentId_ShouldThrowNotFoundException() {
        UUID randomId = UUID.randomUUID();
        given(userRepository.findById(randomId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(randomId))
                .isInstanceOf(UserNotFoundException.class);
    }
}
