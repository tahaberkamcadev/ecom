package com.tahaberkamcadev.projection_service.mapper;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.tahaberkamcadev.projection_service.dto.LatestReviewSnippet;
import com.tahaberkamcadev.projection_service.dto.response.OrderDetailResponse;
import com.tahaberkamcadev.projection_service.dto.response.OrderLineItemResponse;
import com.tahaberkamcadev.projection_service.dto.response.OrderSummaryResponse;
import com.tahaberkamcadev.projection_service.dto.response.ProductDetailResponse;
import com.tahaberkamcadev.projection_service.dto.response.ProductReviewResponse;
import com.tahaberkamcadev.projection_service.dto.response.ProductSummaryResponse;
import com.tahaberkamcadev.projection_service.dto.response.ReviewSnippetResponse;
import com.tahaberkamcadev.projection_service.entity.OrderLineItemView;
import com.tahaberkamcadev.projection_service.entity.OrderView;
import com.tahaberkamcadev.projection_service.entity.ProductReviewView;
import com.tahaberkamcadev.projection_service.entity.ProductView;
import com.tahaberkamcadev.projection_service.service.OrderQueryService.OrderDetailQueryResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogMapper {

    private static final TypeReference<List<LatestReviewSnippet>> LATEST_REVIEWS_TYPE =
            new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public ProductSummaryResponse toSummary(ProductView product) {
        return new ProductSummaryResponse(
                product.getProductId(),
                product.getCategory(),
                product.getName(),
                product.getBrand(),
                product.getPrice(),
                product.isInStock(),
                product.getAverageRating(),
                product.getReviewCount()
        );
    }

    public ProductDetailResponse toDetail(ProductView product) {
        return new ProductDetailResponse(
                product.getProductId(),
                product.getCategory(),
                product.getName(),
                product.getBrand(),
                product.getDescription(),
                product.getPrice(),
                product.isInStock(),
                product.isActive(),
                product.getAverageRating(),
                product.getReviewCount(),
                parseLatestReviews(product.getLatestReviews()),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    public ReviewSnippetResponse toReviewSnippet(ProductReviewView review) {
        return new ReviewSnippetResponse(
                review.getReviewId(),
                review.getUserId(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }

    public ProductReviewResponse toProductReview(ProductReviewView review) {
        return new ProductReviewResponse(
                review.getReviewId(),
                review.getUserId(),
                review.getUserFirstName(),
                review.getUserLastName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }

    public OrderSummaryResponse toOrderSummary(OrderView order) {
        return new OrderSummaryResponse(
                order.getOrderId(),
                order.getStatus(),
                order.getTotalPrice(),
                order.getLineCount(),
                order.getTotalQuantity(),
                order.getSummaryPreview(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    public OrderDetailResponse toOrderDetail(OrderDetailQueryResult result) {
        List<OrderLineItemResponse> lineItems = result.lineItems().stream()
                .map(this::toLineItem)
                .toList();

        return new OrderDetailResponse(toOrderSummary(result.order()), lineItems);
    }

    private OrderLineItemResponse toLineItem(OrderLineItemView lineItem) {
        return new OrderLineItemResponse(
                lineItem.getId().getProductId(),
                lineItem.getProductName(),
                lineItem.getUnitPrice(),
                lineItem.getQuantity(),
                lineItem.getLineTotal()
        );
    }

    private List<ReviewSnippetResponse> parseLatestReviews(String latestReviewsJson) {
        if (latestReviewsJson == null || latestReviewsJson.isBlank()) {
            return List.of();
        }
        try {
            List<LatestReviewSnippet> snippets = objectMapper.readValue(latestReviewsJson, LATEST_REVIEWS_TYPE);
            List<ReviewSnippetResponse> responses = new ArrayList<>(snippets.size());
            for (LatestReviewSnippet snippet : snippets) {
                responses.add(new ReviewSnippetResponse(
                        snippet.reviewId(),
                        snippet.userId(),
                        snippet.rating(),
                        snippet.comment(),
                        snippet.createdAt()
                ));
            }
            return responses;
        } catch (Exception exception) {
            log.warn("Failed to parse latestReviews JSON for API response", exception);
            return List.of();
        }
    }
}
