package com.tahaberkamcadev.projection_service.dto.response;

import java.util.List;

public record ReviewPageResponse(
        List<ReviewSnippetResponse> items,
        long total,
        int page,
        int size
) {
}
