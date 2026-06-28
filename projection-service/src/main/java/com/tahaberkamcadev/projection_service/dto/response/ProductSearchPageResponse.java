package com.tahaberkamcadev.projection_service.dto.response;

import java.util.List;

public record ProductSearchPageResponse(
        List<ProductSummaryResponse> items,
        long total,
        int page,
        int size
) {
}
