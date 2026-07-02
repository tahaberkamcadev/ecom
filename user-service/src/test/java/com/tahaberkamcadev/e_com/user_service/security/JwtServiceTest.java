package com.tahaberkamcadev.e_com.user_service.security;

import com.tahaberkamcadev.e_com.user_service.entity.Role;
import com.tahaberkamcadev.e_com.user_service.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long TEST_EXPIRATION = 86400000L;

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", TEST_EXPIRATION);

        testUser = User.builder()
                .id(UUID.randomUUID())
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .password("encodedPassword")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("Generated token should contain the correct username")
    void generateToken_ShouldContainCorrectUsername() {
        String token = jwtService.generateToken(testUser);

        assertThat(jwtService.extractUsername(token)).isEqualTo(testUser.getEmail());
    }

    @Test
    @DisplayName("Valid token should pass isTokenValid check")
    void isTokenValid_WithValidToken_ShouldReturnTrue() {
        String token = jwtService.generateToken(testUser);

        assertThat(jwtService.isTokenValid(token, testUser)).isTrue();
    }

    @Test
    @DisplayName("Token generated for one user should be invalid for another user")
    void isTokenValid_WithDifferentUser_ShouldReturnFalse() {
        String token = jwtService.generateToken(testUser);

        User anotherUser = User.builder()
                .id(UUID.randomUUID())
                .email("other@example.com")
                .password("encoded")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();

        assertThat(jwtService.isTokenValid(token, anotherUser)).isFalse();
    }

    @Test
    @DisplayName("Expired token should throw ExpiredJwtException")
    void isTokenValid_WithExpiredToken_ShouldThrowExpiredJwtException() {
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1000L);
        String expiredToken = jwtService.generateToken(testUser);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", TEST_EXPIRATION);

        assertThatThrownBy(() -> jwtService.isTokenValid(expiredToken, testUser))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    @DisplayName("getExpirationTime should return the configured value")
    void getExpirationTime_ShouldReturnConfiguredValue() {
        assertThat(jwtService.getExpirationTime()).isEqualTo(TEST_EXPIRATION);
    }

    @Test
    @DisplayName("Token generated for an ADMIN user should contain the correct username")
    void generateToken_ForAdminUser_ShouldContainCorrectUsername() {
        User adminUser = User.builder()
                .id(UUID.randomUUID())
                .email("admin@example.com")
                .password("encodedPassword")
                .role(Role.ADMIN)
                .enabled(true)
                .build();

        String token = jwtService.generateToken(adminUser);

        assertThat(jwtService.extractUsername(token)).isEqualTo("admin@example.com");
        assertThat(jwtService.isTokenValid(token, adminUser)).isTrue();
    }
}
