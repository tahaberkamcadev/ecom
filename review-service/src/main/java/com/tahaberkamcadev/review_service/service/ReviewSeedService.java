package com.tahaberkamcadev.review_service.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tahaberkamcadev.review_service.entity.Review;
import com.tahaberkamcadev.review_service.repository.ReviewRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewSeedService {

    private final ReviewRepository reviewRepository;
    private final OutboxEventService outboxEventService;

    @Transactional
    public void seedDemoReviewsForProduct(UUID productId, int productIndex) {
        if (reviewRepository.existsByProductId(productId)) {
            return;
        }

        int reviewsForProduct = productIndex % 2 == 0 ? 2 : 3;
        int templateOffset = templateOffsetForProduct(productIndex);
        int seeded = 0;

        for (int i = 0; i < reviewsForProduct; i++) {
            ReviewSeedTemplate template = REVIEW_TEMPLATES.get((templateOffset + i) % REVIEW_TEMPLATES.size());

            Review review = Review.builder()
                    .userId(UUID.randomUUID())
                    .userFirstName(template.firstName())
                    .userLastName(template.lastName())
                    .productId(productId)
                    .rating(template.rating())
                    .comment(template.comment())
                    .build();

            Review saved = reviewRepository.save(review);
            outboxEventService.saveReviewCreatedEvent(saved);
            seeded++;
        }

        log.info("Seeded {} demo reviews for product {}", seeded, productId);
    }

    private static int templateOffsetForProduct(int productIndex) {
        int offset = 0;
        for (int i = 0; i < productIndex; i++) {
            offset += i % 2 == 0 ? 2 : 3;
        }
        return offset;
    }

    private record ReviewSeedTemplate(
            String firstName,
            String lastName,
            int rating,
            String comment
    ) {
    }

    private static final List<ReviewSeedTemplate> REVIEW_TEMPLATES = List.of(
            new ReviewSeedTemplate("John", "Doe", 5, "Excellent quality. Exactly what I needed."),
            new ReviewSeedTemplate("Jane", "Doe", 4, "Very good overall, just a tiny packaging issue."),
            new ReviewSeedTemplate("Alex", "Smith", 5, "Fast delivery feel and great value for money."),
            new ReviewSeedTemplate("Maria", "Garcia", 3, "Decent product, does the job but nothing special."),
            new ReviewSeedTemplate("Chris", "Johnson", 4, "Solid build and easy to use every day."),
            new ReviewSeedTemplate("Emily", "Brown", 5, "Love it. Would recommend to friends."),
            new ReviewSeedTemplate("Michael", "Wilson", 2, "Not what I expected from the description."),
            new ReviewSeedTemplate("Sarah", "Davis", 4, "Good experience, minor quirks but still happy."),
            new ReviewSeedTemplate("David", "Miller", 5, "Outstanding. One of my best purchases this year."),
            new ReviewSeedTemplate("Laura", "Taylor", 3, "Average product, fair for the price."),
            new ReviewSeedTemplate("James", "Anderson", 4, "Reliable and well made."),
            new ReviewSeedTemplate("Olivia", "Thomas", 1, "Disappointed. Would not buy again."),
            new ReviewSeedTemplate("Daniel", "Moore", 5, "Perfect fit for my setup."),
            new ReviewSeedTemplate("Sophia", "Martin", 4, "Works great after a week of daily use."),
            new ReviewSeedTemplate("Robert", "Lee", 2, "Quality could be better for this price point.")
    );
}
