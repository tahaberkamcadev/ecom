package com.tahaberkamcadev.e_com.user_service.dto.response;

import com.tahaberkamcadev.e_com.user_service.model.Role;
import com.tahaberkamcadev.e_com.user_service.model.User;
import com.tahaberkamcadev.e_com.user_service.model.UserType;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        Role role,
        UserType userType,
        boolean enabled,
        boolean emailVerified,
        String phoneNumber,
        boolean phoneVerified,
        String preferredLanguage,
        String preferredCurrency,
        boolean marketingConsent,
        boolean smsConsent,
        boolean emailConsent,
        LocalDateTime lastLoginAt,
        Long loginCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.getUserType(),
                user.isEnabled(),
                user.isEmailVerified(),
                user.getPhoneNumber(),
                user.isPhoneVerified(),
                user.getPreferredLanguage(),
                user.getPreferredCurrency(),
                user.isMarketingConsent(),
                user.isSmsConsent(),
                user.isEmailConsent(),
                user.getLastLoginAt(),
                user.getLoginCount(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
