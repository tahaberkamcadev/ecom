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
import com.tahaberkamcadev.e_com.user_service.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    private User savedUser;

    @BeforeEach
    void setUp() {
        savedUser = User.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("$2a$10$encodedPassword")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("Register: valid request should return an auth response with a token")
    void register_WithValidRequest_ShouldReturnAuthResponse() {
        RegisterRequest request = new RegisterRequest("John", "Doe", "John@Example.com", "password123");

        given(userRepository.existsByEmail("john@example.com")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("$2a$10$encodedPassword");
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(jwtService.generateToken(any(), any(User.class))).willReturn("mock.jwt.token");
        given(jwtService.getExpirationTime()).willReturn(86400000L);

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("mock.jwt.token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(86400000L);
    }

    @Test
    @DisplayName("Register: email should be normalized to lowercase and trimmed")
    void register_ShouldNormalizeEmail() {
        RegisterRequest request = new RegisterRequest("John", "Doe", "  JOHN@EXAMPLE.COM  ", "password123");

        given(userRepository.existsByEmail("john@example.com")).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("encoded");
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(jwtService.generateToken(any(), any(User.class))).willReturn("token");
        given(jwtService.getExpirationTime()).willReturn(86400000L);

        authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("Register: existing email should throw EmailAlreadyExistsException")
    void register_WithExistingEmail_ShouldThrowEmailAlreadyExistsException() {
        RegisterRequest request = new RegisterRequest("John", "Doe", "john@example.com", "password123");

        given(userRepository.existsByEmail("john@example.com")).willReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Register: password should be encoded with BCrypt")
    void register_ShouldEncodePassword() {
        RegisterRequest request = new RegisterRequest("John", "Doe", "john@example.com", "plainPassword");

        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(passwordEncoder.encode("plainPassword")).willReturn("$2a$10$hashed");
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(jwtService.generateToken(any(), any(User.class))).willReturn("token");
        given(jwtService.getExpirationTime()).willReturn(86400000L);

        authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("$2a$10$hashed");
    }

    @Test
    @DisplayName("Register: new user should be assigned the CUSTOMER role")
    void register_ShouldAssignCustomerRole() {
        RegisterRequest request = new RegisterRequest("John", "Doe", "john@example.com", "password123");

        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("encoded");
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(jwtService.generateToken(any(), any(User.class))).willReturn("token");
        given(jwtService.getExpirationTime()).willReturn(86400000L);

        authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    @DisplayName("Login: valid credentials should return an auth response with a token")
    void login_WithValidCredentials_ShouldReturnAuthResponse() {
        LoginRequest request = new LoginRequest("john@example.com", "password123");

        given(userRepository.findByEmail("john@example.com")).willReturn(Optional.of(savedUser));
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(jwtService.generateToken(any(), any(User.class))).willReturn("mock.jwt.token");
        given(jwtService.getExpirationTime()).willReturn(86400000L);

        AuthResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("mock.jwt.token");
        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("john@example.com", "password123")
        );
    }

    @Test
    @DisplayName("Login: email should be normalized to lowercase and trimmed")
    void login_ShouldNormalizeEmail() {
        LoginRequest request = new LoginRequest("  JOHN@EXAMPLE.COM  ", "password123");

        given(userRepository.findByEmail("john@example.com")).willReturn(Optional.of(savedUser));
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(jwtService.generateToken(any(), any(User.class))).willReturn("token");
        given(jwtService.getExpirationTime()).willReturn(86400000L);

        authService.login(request);

        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("john@example.com", "password123")
        );
    }

    @Test
    @DisplayName("Login: wrong password should throw BadCredentialsException")
    void login_WithWrongPassword_ShouldThrowBadCredentialsException() {
        LoginRequest request = new LoginRequest("john@example.com", "wrongPassword");

        given(authenticationManager.authenticate(any()))
                .willThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Login: non-existent email should throw UserNotFoundException")
    void login_WithNonExistentUser_ShouldThrowUserNotFoundException() {
        LoginRequest request = new LoginRequest("notfound@example.com", "password");

        given(userRepository.findByEmail("notfound@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UserNotFoundException.class);
    }
}
