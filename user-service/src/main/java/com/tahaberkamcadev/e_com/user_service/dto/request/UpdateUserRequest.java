package com.tahaberkamcadev.e_com.user_service.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(

        @Size(min = 1, message = "First name must be at least 1 character")
        String firstName,

        @Size(min = 1, message = "Last name must be at least 1 character")
        String lastName,

        @Size(min = 8, message = "Password must be at least 8 characters")
        String password
) {
}
