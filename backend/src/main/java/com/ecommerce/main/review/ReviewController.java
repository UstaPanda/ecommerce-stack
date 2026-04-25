package com.ecommerce.main.review;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /** Herkese açık: ürüne ait yorumlar */
    @GetMapping("/product/{productId}")
    public ResponseEntity<Page<ReviewResponse>> getByProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(reviewService.getByProduct(productId, pageable));
    }

    /** Individual: kendi yorumları */
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('ROLE_INDIVIDUAL')")
    public ResponseEntity<Page<ReviewResponse>> getMyReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(reviewService.getMyReviews(auth.getName(), pageable));
    }

    /** Corporate/Admin: store yorumları */
    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasAnyAuthority('ROLE_CORPORATE', 'ROLE_ADMIN')")
    public ResponseEntity<Page<ReviewResponse>> getByStore(
            @PathVariable Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(reviewService.getByStore(storeId, pageable));
    }

    /** Individual: yorum yaz */
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_INDIVIDUAL')")
    public ResponseEntity<ReviewResponse> create(
            @Valid @RequestBody ReviewRequest request,
            Authentication auth) {
        return ResponseEntity.ok(reviewService.create(auth.getName(), request));
    }

    /** Individual: kendi yorumunu güncelle */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_INDIVIDUAL')")
    public ResponseEntity<ReviewResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request,
            Authentication auth) {
        return ResponseEntity.ok(reviewService.update(id, auth.getName(), request));
    }

    /** Individual: kendi yorumunu sil */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_INDIVIDUAL')")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        reviewService.delete(id, auth.getName());
        return ResponseEntity.noContent().build();
    }

    /** Corporate/Admin: yoruma mağaza cevabı ekle */
    @PatchMapping("/{id}/respond")
    @PreAuthorize("hasAnyAuthority('ROLE_CORPORATE', 'ROLE_ADMIN')")
    public ResponseEntity<ReviewResponse> respond(
            @PathVariable Long id,
            @RequestParam String response,
            Authentication auth) {
        return ResponseEntity.ok(reviewService.respond(id, auth.getName(), response));
    }

    /** Debug: ürün için review sayısı */
    @GetMapping("/product/{productId}/count")
    public ResponseEntity<java.util.Map<String, Object>> getReviewCount(@PathVariable Long productId) {
        long count = reviewService.countByProduct(productId);
        return ResponseEntity.ok(java.util.Map.of("productId", productId, "reviewCount", count));
    }

    /** Kayıtlı kullanıcı: helpful vote (kullanıcı başına 1 oy, aynı oy → geri çek) */
    @PostMapping("/{id}/vote")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewResponse> vote(
            @PathVariable Long id,
            @RequestParam boolean helpful,
            Authentication auth) {
        return ResponseEntity.ok(reviewService.vote(id, helpful, auth.getName()));
    }
}
