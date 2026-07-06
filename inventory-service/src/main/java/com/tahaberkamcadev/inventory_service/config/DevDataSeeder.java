package com.tahaberkamcadev.inventory_service.config;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.tahaberkamcadev.inventory_service.client.ReviewSeedClient;
import com.tahaberkamcadev.inventory_service.dto.ProductCategory;
import com.tahaberkamcadev.inventory_service.entity.Product;
import com.tahaberkamcadev.inventory_service.repository.ProductRepository;
import com.tahaberkamcadev.inventory_service.service.ProductService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DevDataSeeder implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final ProductService productService;
    private final ReviewSeedClient reviewSeedClient;

    @Override
    public void run(ApplicationArguments args) {
        if (productRepository.count() > 0) {
            return;
        }

        for (int productIndex = 0; productIndex < PRODUCT_SEEDS.size(); productIndex++) {
            ProductSeed seed = PRODUCT_SEEDS.get(productIndex);
            Product product = Product.builder()
                    .category(seed.category())
                    .name(seed.name())
                    .brand(seed.brand())
                    .description(seed.description())
                    .price(seed.price())
                    .stock(seed.stock())
                    .active(true)
                    .build();
            Product saved = productService.saveProduct(product);
            reviewSeedClient.seedDemoReviews(saved.getId(), productIndex);
        }

        log.info("Seeded {} demo products for local development", PRODUCT_SEEDS.size());
    }

    private record ProductSeed(
            ProductCategory category,
            String name,
            String brand,
            String description,
            BigDecimal price,
            int stock
    ) {
    }

    private static final List<ProductSeed> PRODUCT_SEEDS = List.of(
            new ProductSeed(
                    ProductCategory.ELECTRONICS,
                    "Wireless Headphones",
                    "Sony",
                    "Noise-cancelling over-ear headphones with 30-hour battery life.",
                    new BigDecimal("149.99"),
                    40
            ),
            new ProductSeed(
                    ProductCategory.ELECTRONICS,
                    "Mechanical Keyboard",
                    "Keychron",
                    "Compact wireless mechanical keyboard with hot-swappable switches.",
                    new BigDecimal("89.99"),
                    35
            ),
            new ProductSeed(
                    ProductCategory.ELECTRONICS,
                    "4K Monitor",
                    "Dell",
                    "27-inch 4K IPS monitor for productivity and light gaming.",
                    new BigDecimal("329.99"),
                    20
            ),
            new ProductSeed(
                    ProductCategory.CLOTHING,
                    "Classic Hoodie",
                    "Uniqlo",
                    "Soft cotton hoodie available in multiple colors.",
                    new BigDecimal("49.99"),
                    60
            ),
            new ProductSeed(
                    ProductCategory.CLOTHING,
                    "Running Shoes",
                    "Nike",
                    "Lightweight daily trainers with responsive cushioning.",
                    new BigDecimal("119.99"),
                    45
            ),
            new ProductSeed(
                    ProductCategory.HOME,
                    "Ceramic Coffee Mug",
                    "IKEA",
                    "350 ml matte ceramic mug for hot drinks.",
                    new BigDecimal("12.99"),
                    100
            ),
            new ProductSeed(
                    ProductCategory.BOOKS,
                    "Clean Code",
                    "Prentice Hall",
                    "A handbook of agile software craftsmanship by Robert C. Martin.",
                    new BigDecimal("39.99"),
                    25
            ),
            new ProductSeed(
                    ProductCategory.BOOKS,
                    "Designing Data-Intensive Applications",
                    "O'Reilly",
                    "Principles for reliable, scalable, and maintainable systems.",
                    new BigDecimal("54.99"),
                    30
            ),
            new ProductSeed(
                    ProductCategory.SPORTS,
                    "Yoga Mat",
                    "Lululemon",
                    "Non-slip exercise mat with carrying strap.",
                    new BigDecimal("68.00"),
                    50
            ),
            new ProductSeed(
                    ProductCategory.BEAUTY,
                    "Moisturizing Face Cream",
                    "CeraVe",
                    "Daily facial moisturizer for normal to dry skin.",
                    new BigDecimal("17.50"),
                    80
            )
    );
}
