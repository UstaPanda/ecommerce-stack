package com.ecommerce.main.wishlist;

import com.ecommerce.main.product.Product;
import com.ecommerce.main.product.ProductRepository;
import com.ecommerce.main.user.User;
import com.ecommerce.main.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<WishlistResponse> getMyWishlist(String email) {
        return wishlistRepository.findByUserEmailOrderByAddedAtDesc(email)
                .stream().map(WishlistResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public boolean isWishlisted(String email, Long productId) {
        return wishlistRepository.existsByUserEmailAndProductId(email, productId);
    }

    public WishlistResponse add(String email, Long productId) {
        if (wishlistRepository.existsByUserEmailAndProductId(email, productId)) {
            throw new IllegalStateException("Ürün zaten favorilerde.");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        WishlistItem item = WishlistItem.builder()
                .user(user)
                .product(product)
                .build();
        return WishlistResponse.from(wishlistRepository.save(item));
    }

    public void remove(String email, Long productId) {
        WishlistItem item = wishlistRepository.findByUserEmailAndProductId(email, productId)
                .orElseThrow(() -> new RuntimeException("Favori bulunamadı."));
        wishlistRepository.delete(item);
    }
}
