package com.ecommerce.main.cart;

import com.ecommerce.main.order.OrderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_INDIVIDUAL')")
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(Authentication auth) {
        return ResponseEntity.ok(cartService.getOrCreateCart(auth.getName()));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            @Valid @RequestBody CartItemRequest request,
            Authentication auth) {
        return ResponseEntity.ok(cartService.addItem(auth.getName(), request));
    }

    @PatchMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> updateItem(
            @PathVariable Long cartItemId,
            @RequestParam int quantity,
            Authentication auth) {
        return ResponseEntity.ok(cartService.updateItem(auth.getName(), cartItemId, quantity));
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> removeItem(
            @PathVariable Long cartItemId,
            Authentication auth) {
        return ResponseEntity.ok(cartService.removeItem(auth.getName(), cartItemId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(Authentication auth) {
        cartService.clearCart(auth.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(
            @RequestParam String paymentMethod,
            @RequestParam String shippingAddress,
            @RequestParam(required = false) String txHash,
            @RequestParam(required = false, defaultValue = "1") Integer chainId,
            @RequestParam(required = false) String couponCode,
            Authentication auth) {
        return ResponseEntity.ok(
            cartService.checkout(auth.getName(), paymentMethod, shippingAddress, txHash, chainId, couponCode));
    }
}
