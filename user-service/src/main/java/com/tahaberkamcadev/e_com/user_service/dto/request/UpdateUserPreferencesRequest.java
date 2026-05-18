package com.tahaberkamcadev.e_com.user_service.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserPreferencesRequest(
        @Size(max = 20, message = "Phone number must not exceed 20 characters")
        String phoneNumber,
        
        @Pattern(regexp = "^[a-z]{2}-[A-Z]{2}$", message = "Language must be in format like 'en-US' or 'es-ES'")
        String preferredLanguage,
        
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3-letter code like 'USD', 'EUR', 'GBP'")
        String preferredCurrency,
        
        Boolean marketingConsent,
        
        Boolean smsConsent,
        
        Boolean emailConsent
) {}