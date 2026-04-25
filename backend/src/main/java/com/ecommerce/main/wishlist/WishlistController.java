package com.ecommerce.main.wishlist;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<List<WishlistResponse>> getMyWishlist(Authentication auth) {
        return ResponseEntity.ok(wishlistService.getMyWishlist(auth.getName()));
    }

    @GetMapping("/{productId}/check")
    public ResponseEntity<Map<String, Boolean>> check(
            @PathVariable Long productId, Authentication auth) {
        boolean wished = wishlistService.isWishlisted(auth.getName(), productId);
        return ResponseEntity.ok(Map.of("wishlisted", wished));
    }

    @PostMapping("/{productId}")
    public ResponseEntity<WishlistResponse> add(
            @PathVariable Long productId, Authentication auth) {
        return ResponseEntity.ok(wishlistService.add(auth.getName(), productId));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> remove(
            @PathVariable Long productId, Authentication auth) {
        wishlistService.remove(auth.getName(), productId);
        return ResponseEntity.noContent().build();
    }
}
