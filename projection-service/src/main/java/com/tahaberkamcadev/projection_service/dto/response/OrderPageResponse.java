package com.tahaberkamcadev.projection_service.dto.response;

import java.util.List;

public record OrderPageResponse(
        List<OrderSummaryResponse> items,
        long total,
        int page,
        int size
) {
}
