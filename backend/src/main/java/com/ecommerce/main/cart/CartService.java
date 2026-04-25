package com.ecommerce.main.cart;

import com.ecommerce.main.coupon.Coupon;
import com.ecommerce.main.coupon.CouponRepository;
import com.ecommerce.main.crypto.BlockchainVerificationService;
import com.ecommerce.main.crypto.CryptoVerificationResult;
import com.ecommerce.main.order.*;
import com.ecommerce.main.product.Product;
import com.ecommerce.main.product.ProductRepository;
import com.ecommerce.main.user.User;
import com.ecommerce.main.user.UserRepository;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final BlockchainVerificationService blockchainVerificationService;
    private final CouponRepository couponRepository;

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${app.base-url:http://localhost}")
    private String baseUrl;

    @PostConstruct
    void initStripe() { Stripe.apiKey = stripeSecretKey; }

    /** Mevcut sepeti getir ya da boş sepet döndür */
    @Transactional
    public CartResponse getOrCreateCart(String email) {
        Cart cart = cartRepository.findByUserEmail(email)
                .orElseGet(() -> {
                    User user = findUser(email);
                    return cartRepository.save(Cart.builder().user(user).build());
                });
        return CartResponse.from(cart);
    }

    /** Ürün ekle ya da miktarı artır */
    @Transactional
    public CartResponse addItem(String email, CartItemRequest request) {
        Cart cart = getOrCreateCartEntity(email);
        Product product = findProduct(request.getProductId());

        // Tüm ürünler aynı store'dan olmalı
        if (!cart.getItems().isEmpty()) {
            Long existingStoreId = cart.getItems().get(0).getProduct().getStore().getId();
            if (!product.getStore().getId().equals(existingStoreId)) {
                throw new IllegalStateException(
                        "Cart can only contain products from one store. Clear the cart first.");
            }
        }

        if (product.getStockQuantity() < request.getQuantity()) {
            throw new IllegalStateException("Insufficient stock for: " + product.getName());
        }

        Optional<CartItem> existing = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(product.getId()))
                .findFirst();

        if (existing.isPresent()) {
            existing.get().setQuantity(existing.get().getQuantity() + request.getQuantity());
        } else {
            CartItem item = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cart.getItems().add(item);
        }

        return CartResponse.from(cartRepository.save(cart));
    }

    /** Ürün miktarını güncelle */
    @Transactional
    public CartResponse updateItem(String email, Long cartItemId, int quantity) {
        Cart cart = getOrCreateCartEntity(email);
        CartItem item = findItem(cart, cartItemId);

        if (quantity <= 0) {
            cart.getItems().remove(item);
        } else {
            if (item.getProduct().getStockQuantity() < quantity) {
                throw new IllegalStateException("Insufficient stock for: " + item.getProduct().getName());
            }
            item.setQuantity(quantity);
        }

        return CartResponse.from(cartRepository.save(cart));
    }

    /** Sepetten ürün kaldır */
    @Transactional
    public CartResponse removeItem(String email, Long cartItemId) {
        Cart cart = getOrCreateCartEntity(email);
        cart.getItems().remove(findItem(cart, cartItemId));
        return CartResponse.from(cartRepository.save(cart));
    }

    /** Sepeti temizle */
    @Transactional
    public void clearCart(String email) {
        Cart cart = getOrCreateCartEntity(email);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    /** Checkout: sepeti siparişe dönüştür */
    @Transactional
    public OrderResponse checkout(String email, String paymentMethod, String shippingAddress,
                                  String txHash, int chainId, String couponCode) {
        Cart cart = getOrCreateCartEntity(email);

        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        User user = findUser(email);
        var store = cart.getItems().get(0).getProduct().getStore();

        // ─── Stok kontrolü + toplam hesaplama ────────────────────────────────
        double total = 0.0;
        for (CartItem cartItem : cart.getItems()) {
            if (cartItem.getProduct().getStockQuantity() < cartItem.getQuantity()) {
                throw new IllegalStateException(
                    "Insufficient stock for: " + cartItem.getProduct().getName());
            }
            total += cartItem.getQuantity() * cartItem.getProduct().getUnitPrice();
        }

        // ─── Kupon uygulaması ─────────────────────────────────────────────────
        if (couponCode != null && !couponCode.isBlank()) {
            Coupon coupon = couponRepository.findByCodeIgnoreCase(couponCode.trim())
                    .orElseThrow(() -> new IllegalArgumentException("Geçersiz kupon kodu."));
            if (!coupon.getActive()) throw new IllegalArgumentException("Kupon artık aktif değil.");
            if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(LocalDateTime.now()))
                throw new IllegalArgumentException("Kuponun süresi dolmuş.");
            if (coupon.getMaxUses() != null && coupon.getUsedCount() >= coupon.getMaxUses())
                throw new IllegalArgumentException("Kupon kullanım limiti dolmuş.");
            if (coupon.getStore() != null && !coupon.getStore().getId().equals(store.getId()))
                throw new IllegalArgumentException("Bu kupon bu mağaza için geçerli değil.");

            if ("PERCENTAGE".equals(coupon.getDiscountType())) {
                total = total * (1.0 - coupon.getDiscountValue().doubleValue() / 100.0);
            } else {
                total = Math.max(0, total - coupon.getDiscountValue().doubleValue());
            }
            coupon.setUsedCount(coupon.getUsedCount() + 1);
            couponRepository.save(coupon);
        }

        // ─── Stripe charge ────────────────────────────────────────────────────
        // paymentMethod: "STRIPE_PM:pm_xxx" (yeni kart) veya "STRIPE:pm_xxx:4242" (kayıtlı)
        if (paymentMethod.startsWith("STRIPE")) {
            String pmId = extractStripePaymentMethodId(paymentMethod);
            if (pmId == null || pmId.isBlank()) {
                throw new IllegalStateException("Geçerli bir Stripe ödeme yöntemi bulunamadı.");
            }
            try {
                long amountCents = Math.round(total * 100);
                PaymentIntent intent = PaymentIntent.create(
                    PaymentIntentCreateParams.builder()
                        .setAmount(amountCents)
                        .setCurrency("usd")
                        .setPaymentMethod(pmId)
                        .setConfirm(true)
                        .setReturnUrl(baseUrl + "/orders")
                        .build()
                );
                if (!"succeeded".equals(intent.getStatus())) {
                    throw new IllegalStateException("Stripe ödemesi tamamlanamadı: " + intent.getStatus());
                }
            } catch (com.stripe.exception.StripeException e) {
                throw new IllegalStateException("Stripe hatası: " + e.getMessage());
            }
        }

        // ─── Sipariş başlangıç durumu ─────────────────────────────────────────
        OrderStatus initialStatus = paymentMethod.startsWith("STRIPE") ? OrderStatus.CONFIRMED : OrderStatus.PENDING;
        if (paymentMethod.startsWith("CRYPTO_WALLET")) {
            if (txHash == null || txHash.isBlank()) {
                throw new IllegalStateException(
                    "Kripto ödeme için blockchain işlem hash'i (txHash) gereklidir.");
            }
            CryptoVerificationResult result =
                blockchainVerificationService.verify(txHash, chainId, total);
            if (!result.isSuccess()) {
                throw new IllegalStateException(result.getMessage());
            }
            initialStatus = OrderStatus.CONFIRMED;
        }

        // ─── Sipariş oluştur ──────────────────────────────────────────────────
        Order order = Order.builder()
                .user(user)
                .store(store)
                .paymentMethod(paymentMethod)
                .shippingAddress(shippingAddress)
                .status(initialStatus)
                .grandTotal(total)
                .txHash(txHash)
                .build();

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .unitPrice(product.getUnitPrice())
                    .build();
            order.getItems().add(orderItem);
        }

        Order saved = orderRepository.save(order);

        // Sepeti temizle
        cart.getItems().clear();
        cartRepository.save(cart);

        return OrderResponse.from(saved);
    }

    private Cart getOrCreateCartEntity(String email) {
        return cartRepository.findByUserEmail(email)
                .orElseGet(() -> {
                    User user = findUser(email);
                    return cartRepository.save(Cart.builder().user(user).build());
                });
    }

    private CartItem findItem(Cart cart, Long cartItemId) {
        return cart.getItems().stream()
                .filter(i -> i.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found: " + cartItemId));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
    }

    /**
     * "STRIPE_PM:pm_xxx"        → "pm_xxx"
     * "STRIPE:pm_xxx:4242"      → "pm_xxx"
     * "STRIPE:Visa:4242"        → null  (eski format, pm_id yok)
     */
    private String extractStripePaymentMethodId(String paymentMethod) {
        String[] parts = paymentMethod.split(":", 3);
        if (parts.length < 2) return null;
        String candidate = parts[1];
        return candidate.startsWith("pm_") ? candidate : null;
    }
}
