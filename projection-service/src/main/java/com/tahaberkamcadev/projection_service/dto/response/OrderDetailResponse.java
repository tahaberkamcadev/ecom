package com.tahaberkamcadev.projection_service.dto.response;

import java.util.List;

public record OrderDetailResponse(
        OrderSummaryResponse order,
        List<OrderLineItemResponse> lineItems
) {
}
