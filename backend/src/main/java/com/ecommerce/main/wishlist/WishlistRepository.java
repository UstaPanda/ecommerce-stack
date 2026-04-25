package com.ecommerce.main.wishlist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<WishlistItem, Long> {

    List<WishlistItem> findByUserEmailOrderByAddedAtDesc(String email);

    Optional<WishlistItem> findByUserEmailAndProductId(String email, Long productId);

    boolean existsByUserEmailAndProductId(String email, Long productId);

    @Query("SELECT COUNT(w) FROM WishlistItem w WHERE w.user.email = :email")
    long countByUserEmail(@Param("email") String email);
}
