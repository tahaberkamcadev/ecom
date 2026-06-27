package com.tahaberkamcadev.projection_service.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tahaberkamcadev.projection_service.dto.response.ProductDetailResponse;
import com.tahaberkamcadev.projection_service.dto.response.ProductSummaryResponse;
import com.tahaberkamcadev.projection_service.entity.ProductView;
import com.tahaberkamcadev.projection_service.enums.ProductCategory;
import com.tahaberkamcadev.projection_service.mapper.CatalogMapper;
import com.tahaberkamcadev.projection_service.service.ProductQueryService;

@WebMvcTest(controllers = ProductCatalogController.class)
class ProductCatalogControllerTest {

    private static final String GATEWAY_SECRET = "test-secret";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductQueryService productQueryService;

    @MockitoBean
    private CatalogMapper catalogMapper;

    @Test
    void listProducts_returnsCatalogSummaries() throws Exception {
        ProductView productView = ProductView.builder()
                .productId(UUID.randomUUID())
                .category(ProductCategory.ELECTRONICS)
                .name("Phone")
                .brand("Acme")
                .price(new BigDecimal("999.00"))
                .inStock(true)
                .active(true)
                .averageRating(new BigDecimal("4.50"))
                .reviewCount(2)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ProductSummaryResponse summary = new ProductSummaryResponse(
                productView.getProductId(),
                productView.getCategory(),
                productView.getName(),
                productView.getBrand(),
                productView.getPrice(),
                productView.isInStock(),
                productView.getAverageRating(),
                productView.getReviewCount()
        );

        when(productQueryService.findByCategoryAndActive(eq(ProductCategory.ELECTRONICS), eq(true)))
                .thenReturn(List.of(productView));
        when(catalogMapper.toSummary(productView)).thenReturn(summary);

        mockMvc.perform(get("/api/catalog/products")
                        .param("category", "ELECTRONICS")
                        .header("X-Gateway-Secret", GATEWAY_SECRET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Phone"))
                .andExpect(jsonPath("$[0].category").value("ELECTRONICS"));
    }

    @Test
    void getProduct_returnsNotFoundWhenMissing() throws Exception {
        UUID productId = UUID.randomUUID();
        when(productQueryService.findProductById(productId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/catalog/products/{productId}", productId)
                        .header("X-Gateway-Secret", GATEWAY_SECRET))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void getProduct_returnsDetailWhenFound() throws Exception {
        UUID productId = UUID.randomUUID();
        ProductView productView = ProductView.builder()
                .productId(productId)
                .category(ProductCategory.BOOKS)
                .name("Clean Code")
                .brand("Prentice Hall")
                .description("A handbook of agile software craftsmanship")
                .price(new BigDecimal("39.99"))
                .inStock(true)
                .active(true)
                .averageRating(new BigDecimal("4.80"))
                .reviewCount(10)
                .latestReviews("[]")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ProductDetailResponse detail = new ProductDetailResponse(
                productId,
                ProductCategory.BOOKS,
                "Clean Code",
                "Prentice Hall",
                "A handbook of agile software craftsmanship",
                new BigDecimal("39.99"),
                true,
                true,
                new BigDecimal("4.80"),
                10,
                List.of(),
                productView.getCreatedAt(),
                productView.getUpdatedAt()
        );

        when(productQueryService.findProductById(productId)).thenReturn(Optional.of(productView));
        when(catalogMapper.toDetail(productView)).thenReturn(detail);

        mockMvc.perform(get("/api/catalog/products/{productId}", productId)
                        .header("X-Gateway-Secret", GATEWAY_SECRET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Clean Code"))
                .andExpect(jsonPath("$.reviewCount").value(10));
    }
}
