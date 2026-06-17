package com.tahaberkamcadev.review_service.dto;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateReviewRequest(
        @NotNull 
        UUID productId,
        @Min(1) @Max(5)
        int rating,
        @NotBlank
         String comment
) {}
