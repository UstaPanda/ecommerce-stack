package com.ecommerce.main.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByProductId(Long productId, Pageable pageable);
    Page<Review> findByUserEmail(String email, Pageable pageable);
    Page<Review> findByProductStoreId(Long storeId, Pageable pageable);
    Optional<Review> findByUserEmailAndProductId(String email, Long productId);
    boolean existsByUserEmailAndProductId(String email, Long productId);

    @Query(
        value = """
            SELECT new com.ecommerce.main.review.ReviewResponse(
                r.id, u.id, u.name,
                p.id, p.name,
                r.starRating, r.comment,
                r.helpfulVotes, r.totalVotes,
                r.sentiment, r.storeResponse, r.storeRespondedAt, r.createdAt
            )
            FROM Review r JOIN r.user u JOIN r.product p
            WHERE p.id = :productId
            """,
        countQuery = "SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId"
    )
    Page<ReviewResponse> findDtosByProductId(@Param("productId") Long productId, Pageable pageable);

    @Query(
        value = """
            SELECT new com.ecommerce.main.review.ReviewResponse(
                r.id, u.id, u.name,
                p.id, p.name,
                r.starRating, r.comment,
                r.helpfulVotes, r.totalVotes,
                r.sentiment, r.storeResponse, r.storeRespondedAt, r.createdAt
            )
            FROM Review r JOIN r.user u JOIN r.product p
            WHERE u.email = :email
            """,
        countQuery = "SELECT COUNT(r) FROM Review r WHERE r.user.email = :email"
    )
    Page<ReviewResponse> findDtosByUserEmail(@Param("email") String email, Pageable pageable);

    @Query(
        value = """
            SELECT new com.ecommerce.main.review.ReviewResponse(
                r.id, u.id, u.name,
                p.id, p.name,
                r.starRating, r.comment,
                r.helpfulVotes, r.totalVotes,
                r.sentiment, r.storeResponse, r.storeRespondedAt, r.createdAt
            )
            FROM Review r JOIN r.user u JOIN r.product p
            WHERE p.store.id = :storeId
            """,
        countQuery = "SELECT COUNT(r) FROM Review r WHERE r.product.store.id = :storeId"
    )
    Page<ReviewResponse> findDtosByStoreId(@Param("storeId") Long storeId, Pageable pageable);

    // ── Native SQL queries (Hibernate entity mapping bypass) ─────────────────

    @Query(value = """
            SELECT r.id, r.user_id, u.name, r.product_id, p.name,
                   r.star_rating, r.comment, r.helpful_votes, r.total_votes,
                   r.sentiment, r.store_response, r.store_responded_at, r.created_at
            FROM reviews r
            JOIN users u ON r.user_id = u.id
            JOIN products p ON r.product_id = p.id
            WHERE r.product_id = :productId
            ORDER BY r.created_at DESC
            LIMIT :size OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> findRawByProductId(@Param("productId") Long productId,
                                      @Param("size") int size,
                                      @Param("offset") int offset);

    @Query(value = "SELECT COUNT(*) FROM reviews WHERE product_id = :productId", nativeQuery = true)
    long countRawByProductId(@Param("productId") Long productId);

    @Query(value = """
            SELECT r.id, r.user_id, u.name, r.product_id, p.name,
                   r.star_rating, r.comment, r.helpful_votes, r.total_votes,
                   r.sentiment, r.store_response, r.store_responded_at, r.created_at
            FROM reviews r
            JOIN users u ON r.user_id = u.id
            JOIN products p ON r.product_id = p.id
            WHERE u.email = :email
            ORDER BY r.created_at DESC
            LIMIT :size OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> findRawByUserEmail(@Param("email") String email,
                                      @Param("size") int size,
                                      @Param("offset") int offset);

    @Query(value = "SELECT COUNT(*) FROM reviews r JOIN users u ON r.user_id = u.id WHERE u.email = :email", nativeQuery = true)
    long countRawByUserEmail(@Param("email") String email);

    @Query(value = """
            SELECT r.id, r.user_id, u.name, r.product_id, p.name,
                   r.star_rating, r.comment, r.helpful_votes, r.total_votes,
                   r.sentiment, r.store_response, r.store_responded_at, r.created_at
            FROM reviews r
            JOIN users u ON r.user_id = u.id
            JOIN products p ON r.product_id = p.id
            WHERE p.store_id = :storeId
            ORDER BY r.created_at DESC
            LIMIT :size OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> findRawByStoreId(@Param("storeId") Long storeId,
                                    @Param("size") int size,
                                    @Param("offset") int offset);

    @Query(value = """
            SELECT COUNT(*) FROM reviews r
            JOIN products p ON r.product_id = p.id
            WHERE p.store_id = :storeId
            """, nativeQuery = true)
    long countRawByStoreId(@Param("storeId") Long storeId);

    // ── Aggregates ────────────────────────────────────────────────────────────

    @Query("SELECT AVG(r.starRating) FROM Review r WHERE r.product.id = :productId")
    Double avgRatingByProduct(@Param("productId") Long productId);

    long countByProductId(Long productId);

    // Batch: avg rating + review count for a list of product IDs in one query
    @Query("SELECT r.product.id, AVG(r.starRating), COUNT(r) FROM Review r WHERE r.product.id IN :ids GROUP BY r.product.id")
    List<Object[]> avgAndCountByProductIds(@Param("ids") List<Long> ids);

    @Query("SELECT AVG(r.starRating) FROM Review r WHERE r.product.store.id = :storeId")
    Double avgRatingByStore(@Param("storeId") Long storeId);

    @Query("SELECT AVG(r.starRating) FROM Review r WHERE r.product.store.id = :storeId " +
           "AND r.createdAt >= :from AND r.createdAt <= :to")
    Double avgRatingByStoreAndDateRange(@Param("storeId") Long storeId, 
                                         @Param("from") java.time.LocalDateTime from, 
                                         @Param("to") java.time.LocalDateTime to);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.store.id = :storeId")
    long countReviewsByStore(@Param("storeId") Long storeId);
}
