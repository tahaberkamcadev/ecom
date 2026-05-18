package com.tahaberkamcadev.e_com.user_service.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;
    
    @Builder.Default
    @Column(name = "email_verified")
    private boolean emailVerified = false;
    
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;
    
    @Builder.Default
    @Column(name = "phone_verified")
    private boolean phoneVerified = false;
    
    @Column(name = "date_of_birth")
    private LocalDateTime dateOfBirth;
    
    @Column(name = "gender", length = 10)
    private String gender; // MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY
    
    @Builder.Default
    @Column(name = "preferred_language", length = 5)
    private String preferredLanguage = "en-US";
    
    @Builder.Default
    @Column(name = "preferred_currency", length = 3)
    private String preferredCurrency = "USD";
    
    @Builder.Default
    @Column(name = "marketing_consent")
    private boolean marketingConsent = false;
    
    @Builder.Default
    @Column(name = "sms_consent")
    private boolean smsConsent = false;
    
    @Builder.Default
    @Column(name = "email_consent")
    private boolean emailConsent = true;
    
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "user_type")
    private UserType userType = UserType.REGULAR;
    
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    
    @Builder.Default
    @Column(name = "login_count")
    private Long loginCount = 0L;
    
    @Column(name = "verification_token")
    private String verificationToken;
    
    @Column(name = "verification_token_expires_at")
    private LocalDateTime verificationTokenExpiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
