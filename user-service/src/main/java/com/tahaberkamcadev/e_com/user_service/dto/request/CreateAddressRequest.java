package com.tahaberkamcadev.e_com.user_service.dto.request;

import com.tahaberkamcadev.e_com.user_service.model.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAddressRequest(
        @NotNull(message = "Address type is required")
        AddressType addressType,
        
        @NotBlank(message = "Address title is required")
        @Size(max = 100, message = "Address title must not exceed 100 characters")
        String title,
        
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,
        
        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,
        
        @NotBlank(message = "Phone is required")
        @Size(max = 20, message = "Phone must not exceed 20 characters")
        String phone,
        
        @NotBlank(message = "Address line 1 is required")
        @Size(max = 500, message = "Address line 1 must not exceed 500 characters")
        String addressLine1,
        
        @Size(max = 500, message = "Address line 2 must not exceed 500 characters")
        String addressLine2,
        
        @NotBlank(message = "Neighborhood is required")
        @Size(max = 100, message = "Neighborhood must not exceed 100 characters")
        String neighborhood,
        
        @NotBlank(message = "District is required")
        @Size(max = 100, message = "District must not exceed 100 characters")
        String district,
        
        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City must not exceed 100 characters")
        String city,
        
        @NotBlank(message = "Postal code is required")
        @Size(max = 10, message = "Postal code must not exceed 10 characters")
        String postalCode,
        
        @NotBlank(message = "Country is required")
        @Size(max = 100, message = "Country must not exceed 100 characters")
        String country,
        
        boolean isDefault
) {}