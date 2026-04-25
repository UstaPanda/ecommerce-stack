package com.ecommerce.main.review;

import com.ecommerce.main.product.Product;
import com.ecommerce.main.product.ProductRepository;
import com.ecommerce.main.user.Role;
import com.ecommerce.main.user.User;
import com.ecommerce.main.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewVoteRepository reviewVoteRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /** Individual: yorum yaz */
    @Transactional
    public ReviewResponse create(String email, ReviewRequest request) {
        if (reviewRepository.existsByUserEmailAndProductId(email, request.getProductId())) {
            throw new IllegalStateException("You have already reviewed this product");
        }

        User user = findUser(email);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + request.getProductId()));

        String sentiment = deriveSentiment(request.getStarRating());

        Review review = Review.builder()
                .user(user)
                .product(product)
                .starRating(request.getStarRating())
                .comment(request.getComment())
                .sentiment(sentiment)
                .build();

        return ReviewResponse.from(reviewRepository.save(review));
    }

    /** Individual: kendi yorumunu güncelle */
    @Transactional
    public ReviewResponse update(Long id, String email, ReviewRequest request) {
        Review review = findReview(id);
        assertOwner(review, email);

        review.setStarRating(request.getStarRating());
        review.setComment(request.getComment());
        review.setSentiment(deriveSentiment(request.getStarRating()));

        return ReviewResponse.from(reviewRepository.save(review));
    }

    /** Individual: kendi yorumunu sil */
    @Transactional
    public void delete(Long id, String email) {
        Review review = findReview(id);
        assertOwner(review, email);
        reviewRepository.delete(review);
    }

    /** Herkese açık: ürüne ait yorumları listele */
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getByProduct(Long productId, Pageable pageable) {
        long count = reviewRepository.countRawByProductId(productId);
        List<ReviewResponse> content = reviewRepository
                .findRawByProductId(productId, pageable.getPageSize(), (int) pageable.getOffset())
                .stream().map(this::mapRow).toList();
        return new PageImpl<>(content, pageable, count);
    }

    /** Individual: kendi yorumlarını listele */
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getMyReviews(String email, Pageable pageable) {
        long count = reviewRepository.countRawByUserEmail(email);
        List<ReviewResponse> content = reviewRepository
                .findRawByUserEmail(email, pageable.getPageSize(), (int) pageable.getOffset())
                .stream().map(this::mapRow).toList();
        return new PageImpl<>(content, pageable, count);
    }

    /** Corporate/Admin: store'a ait tüm yorumlar */
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getByStore(Long storeId, Pageable pageable) {
        long count = reviewRepository.countRawByStoreId(storeId);
        List<ReviewResponse> content = reviewRepository
                .findRawByStoreId(storeId, pageable.getPageSize(), (int) pageable.getOffset())
                .stream().map(this::mapRow).toList();
        return new PageImpl<>(content, pageable, count);
    }

    /** Corporate/Admin: yoruma mağaza cevabı ekle */
    @Transactional
    public ReviewResponse respond(Long id, String email, String response) {
        Review review = findReview(id);
        User user = findUser(email);

        boolean isAdmin = user.getRoleType() == Role.ADMIN;
        boolean isStoreOwner = review.getProduct().getStore().getOwner().getEmail().equals(email);
        if (!isAdmin && !isStoreOwner) {
            throw new IllegalStateException("Access denied");
        }

        review.setStoreResponse(response);
        review.setStoreRespondedAt(LocalDateTime.now());
        return ReviewResponse.from(reviewRepository.save(review));
    }

    /**
     * Kullanıcı başına 1 oy: aynı oy tekrar basılırsa geri çekilir, farklı oya geçilirse değiştirilir.
     */
    @Transactional
    public ReviewResponse vote(Long id, boolean helpful, String email) {
        Review review = findReview(id);
        User user = findUser(email);

        reviewVoteRepository.findByReviewIdAndUserEmail(id, email).ifPresentOrElse(
            existing -> {
                if (existing.isHelpful() == helpful) {
                    // Aynı oy → geri çek
                    if (helpful) review.setHelpfulVotes(Math.max(0, review.getHelpfulVotes() - 1));
                    review.setTotalVotes(Math.max(0, review.getTotalVotes() - 1));
                    reviewVoteRepository.delete(existing);
                } else {
                    // Farklı oya geç
                    existing.setHelpful(helpful);
                    if (helpful) {
                        review.setHelpfulVotes(review.getHelpfulVotes() + 1);
                    } else {
                        review.setHelpfulVotes(Math.max(0, review.getHelpfulVotes() - 1));
                    }
                    reviewVoteRepository.save(existing);
                }
            },
            () -> {
                // Yeni oy
                ReviewVote vote = ReviewVote.builder()
                        .review(review)
                        .user(user)
                        .helpful(helpful)
                        .build();
                reviewVoteRepository.save(vote);
                review.setTotalVotes(review.getTotalVotes() + 1);
                if (helpful) review.setHelpfulVotes(review.getHelpfulVotes() + 1);
            }
        );

        return ReviewResponse.from(reviewRepository.save(review));
    }

    /** Debug: ürün için review sayısı */
    @Transactional(readOnly = true)
    public long countByProduct(Long productId) {
        return reviewRepository.countRawByProductId(productId);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ReviewResponse mapRow(Object[] row) {
        return new ReviewResponse(
                toLong(row[0]),          // id
                toLong(row[1]),          // userId
                (String) row[2],         // userName
                toLong(row[3]),          // productId
                (String) row[4],         // productName
                toInt(row[5]),           // starRating
                (String) row[6],         // comment
                toInt(row[7]),           // helpfulVotes
                toInt(row[8]),           // totalVotes
                (String) row[9],         // sentiment
                (String) row[10],        // storeResponse
                toLocalDateTime(row[11]),// storeRespondedAt
                toLocalDateTime(row[12]) // createdAt
        );
    }

    private Long toLong(Object val) {
        return val == null ? null : ((Number) val).longValue();
    }

    private Integer toInt(Object val) {
        return val == null ? 0 : ((Number) val).intValue();
    }

    private LocalDateTime toLocalDateTime(Object val) {
        if (val == null) return null;
        if (val instanceof LocalDateTime ldt) return ldt;
        if (val instanceof Timestamp ts) return ts.toLocalDateTime();
        return null;
    }

    private String deriveSentiment(int starRating) {
        if (starRating >= 4) return "POSITIVE";
        if (starRating == 3) return "NEUTRAL";
        return "NEGATIVE";
    }

    private Review findReview(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + id));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
    }

    private void assertOwner(Review review, String email) {
        if (!review.getUser().getEmail().equals(email)) {
            throw new IllegalStateException("Access denied");
        }
    }
}
