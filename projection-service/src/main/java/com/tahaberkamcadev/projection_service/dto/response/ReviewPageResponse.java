package com.tahaberkamcadev.projection_service.dto.response;

import java.util.List;

public record ReviewPageResponse(
        List<ProductReviewResponse> items,
        long total,
        int page,
        int size
) {
}
