package com.ecommerce.main.review;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long userId,
        String userName,
        Long productId,
        String productName,
        Integer starRating,
        String comment,
        Integer helpfulVotes,
        Integer totalVotes,
        String sentiment,
        String storeResponse,
        LocalDateTime storeRespondedAt,
        LocalDateTime createdAt
) {
    public static ReviewResponse from(Review r) {
        return new ReviewResponse(
                r.getId(),
                r.getUser().getId(),
                r.getUser().getName(),
                r.getProduct().getId(),
                r.getProduct().getName(),
                r.getStarRating(),
                r.getComment(),
                r.getHelpfulVotes(),
                r.getTotalVotes(),
                r.getSentiment(),
                r.getStoreResponse(),
                r.getStoreRespondedAt(),
                r.getCreatedAt()
        );
    }
}
