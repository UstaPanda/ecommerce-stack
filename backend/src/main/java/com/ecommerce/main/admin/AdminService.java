package com.ecommerce.main.admin;

import com.ecommerce.main.category.Category;
import com.ecommerce.main.category.CategoryRepository;
import com.ecommerce.main.coupon.Coupon;
import com.ecommerce.main.coupon.CouponDto;
import com.ecommerce.main.coupon.CouponRepository;
import com.ecommerce.main.order.Order;
import com.ecommerce.main.order.OrderRepository;
import com.ecommerce.main.order.OrderResponse;
import com.ecommerce.main.order.OrderStatus;
import com.ecommerce.main.product.Product;
import com.ecommerce.main.product.ProductRepository;
import com.ecommerce.main.product.ProductResponse;
import com.ecommerce.main.review.ReviewRepository;
import com.ecommerce.main.store.Store;
import com.ecommerce.main.store.StoreRepository;
import com.ecommerce.main.store.StoreStatus;
import com.ecommerce.main.user.Role;
import com.ecommerce.main.user.User;
import com.ecommerce.main.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final PlatformSettingsRepository settingsRepository;
    private final CategoryRepository categoryRepository;
    private final CouponRepository couponRepository;

    @Transactional(readOnly = true)
    public AdminAnalyticsResponse getPlatformAnalytics() {
        // Get current user to check role for scoped access
        org.springframework.security.core.Authentication auth = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null && user.getRoleType() == Role.CORPORATE) {
            // For Corporate users, filter every stat to their specific store
            List<Store> myStores = storeRepository.findByOwnerEmail(email);
            if (!myStores.isEmpty()) {
                Store s = myStores.get(0);
                long storeOrders = orderRepository.countByStoreId(s.getId());
                long storeProducts = productRepository.countByStoreId(s.getId());
                Double storeRevenue = orderRepository.sumRevenueByStore(s.getId());
                
                return new AdminAnalyticsResponse(
                    0, 0, 0, // Hidden for Corporate
                    1, 1, 0, // Active Store
                    storeOrders, 0, 0, // Orders
                    storeRevenue != null ? storeRevenue : 0.0,
                    storeProducts, 0, // Products/Reviews
                    List.of(new AdminAnalyticsResponse.StoreComparisonEntry(s.getId(), s.getName(), storeOrders, storeRevenue != null ? storeRevenue : 0.0))
                );
            }
        }

        // --- Standard Admin Logic ---
        long totalUsers      = userRepository.count();
        long individualUsers = userRepository.countByRoleType(Role.INDIVIDUAL);
        long corporateUsers  = userRepository.countByRoleType(Role.CORPORATE);
        long totalStores     = storeRepository.count();
        long activeStores    = storeRepository.countByStatus(StoreStatus.OPEN);
        long pendingStores   = storeRepository.countByStatus(StoreStatus.PENDING);
        long totalOrders     = orderRepository.count();
        long pendingOrders   = orderRepository.countByStatus(OrderStatus.PENDING);
        long cancelledOrders = orderRepository.countByStatus(OrderStatus.CANCELLED);
        long returnedOrders  = orderRepository.countByStatus(OrderStatus.RETURNED);
        
        // Net Revenue (Excluding Cancelled and Returned)
        Double revenueRaw    = orderRepository.sumNetRevenue();
        double totalRevenue  = revenueRaw != null ? revenueRaw : 0.0;
        
        long totalProducts   = productRepository.count();
        long totalReviews    = reviewRepository.count();

        List<AdminAnalyticsResponse.StoreComparisonEntry> storeComparison =
                storeRepository.storeRevenueComparison().stream()
                        .map(row -> new AdminAnalyticsResponse.StoreComparisonEntry(
                                ((Number) row[0]).longValue(),
                                (String) row[1],
                                ((Number) row[2]).longValue(),
                                ((Number) row[3]).doubleValue()
                        ))
                        .toList();

        return new AdminAnalyticsResponse(
                totalUsers, individualUsers, corporateUsers,
                totalStores, activeStores, pendingStores,
                totalOrders, pendingOrders, cancelledOrders,
                totalRevenue, totalProducts, totalReviews,
                storeComparison
        );
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(Role role, String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isBlank()) {
            return userRepository.searchUsers(role, keyword, pageable).map(UserResponse::from);
        } else if (role != null) {
            return userRepository.findByRoleType(role, pageable).map(UserResponse::from);
        } else {
            return userRepository.findAll(pageable).map(UserResponse::from);
        }
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return UserResponse.from(findUser(id));
    }

    @Transactional
    public UserResponse suspendUser(Long id) {
        User user = findUser(id);
        user.setLockedUntil(java.time.LocalDateTime.now().plusYears(100));
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse unsuspendUser(Long id) {
        User user = findUser(id);
        user.setLockedUntil(null);
        user.setFailedLoginAttempts(0);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.delete(findUser(id));
    }

    @Transactional
    public UserResponse changeRole(Long id, Role newRole) {
        User user = findUser(id);
        user.setRoleType(newRole);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public PlatformSettings getSettings() {
        return settingsRepository.findById(1L).orElseGet(() ->
                settingsRepository.save(PlatformSettings.builder().id(1L).build()));
    }

    @Transactional
    public PlatformSettings updateSettings(PlatformSettings incoming) {
        incoming.setId(1L);
        return settingsRepository.save(incoming);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    // ── Store Management ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<AdminStoreDto> listStores(String keyword, Pageable pageable) {
        return storeRepository.findAll(pageable).map(AdminStoreDto::from);
    }

    @Transactional
    public AdminStoreDto updateStoreStatus(Long storeId, StoreStatus status) {
        var store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));
        store.setStatus(status);
        return AdminStoreDto.from(storeRepository.save(store));
    }

    // ── Category Management ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        Map<Long, Long> countMap = productRepository.countByCategory().stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).longValue()
                ));
        return categories.stream()
                .map(cat -> new CategoryDto(
                        cat.getId(),
                        cat.getName(),
                        cat.getParent() != null ? cat.getParent().getId() : null,
                        cat.getParent() != null ? cat.getParent().getName() : null,
                        countMap.getOrDefault(cat.getId(), 0L)
                ))
                .toList();
    }

    @Transactional
    public CategoryDto createCategory(String name, Long parentId) {
        Category category = new Category();
        category.setName(name);
        if (parentId != null) {
            Category parent = categoryRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("Parent category not found: " + parentId));
            category.setParent(parent);
        }
        Category saved = categoryRepository.save(category);
        return new CategoryDto(saved.getId(), saved.getName(),
                saved.getParent() != null ? saved.getParent().getId() : null,
                saved.getParent() != null ? saved.getParent().getName() : null, 0L);
    }

    @Transactional
    public CategoryDto updateCategory(Long id, String name, Long parentId) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
        category.setName(name);
        if (parentId != null) {
            Category parent = categoryRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("Parent category not found: " + parentId));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }
        Category saved = categoryRepository.save(category);
        return new CategoryDto(saved.getId(), saved.getName(),
                saved.getParent() != null ? saved.getParent().getId() : null,
                saved.getParent() != null ? saved.getParent().getName() : null, 0L);
    }

    @Transactional
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }

    // ── Coupon Management ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<CouponDto> listCoupons(Pageable pageable) {
        return couponRepository.findAll(pageable).map(CouponDto::from);
    }

    @Transactional
    public CouponDto createCoupon(Map<String, Object> body) {
        String code        = ((String) body.get("code")).toUpperCase().trim();
        String type        = (String) body.get("discountType");
        BigDecimal value   = new BigDecimal(body.get("discountValue").toString());
        Long storeId       = body.get("storeId") != null ? ((Number) body.get("storeId")).longValue() : null;
        Integer maxUses    = body.get("maxUses") != null ? ((Number) body.get("maxUses")).intValue() : null;
        String expiresAtStr = (String) body.get("expiresAt");

        if (couponRepository.findByCodeIgnoreCase(code).isPresent()) {
            throw new IllegalArgumentException("Coupon code already exists: " + code);
        }

        Store store = null;
        if (storeId != null) {
            store = storeRepository.findById(storeId)
                    .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));
        }

        Coupon coupon = Coupon.builder()
                .code(code)
                .discountType(type)
                .discountValue(value)
                .store(store)
                .maxUses(maxUses)
                .active(true)
                .expiresAt(expiresAtStr != null && !expiresAtStr.isBlank()
                        ? LocalDateTime.parse(expiresAtStr) : null)
                .build();

        return CouponDto.from(couponRepository.save(coupon));
    }

    @Transactional
    public void deleteCoupon(Long id) {
        couponRepository.deleteById(id);
    }

    @Transactional
    public CouponDto toggleCoupon(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found: " + id));
        coupon.setActive(!coupon.getActive());
        return CouponDto.from(couponRepository.save(coupon));
    }

    // ── Order Management ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<OrderResponse> listAllOrders(String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            OrderStatus os = OrderStatus.valueOf(status.toUpperCase());
            return orderRepository.findByStatus(os, pageable).map(OrderResponse::from);
        }
        return orderRepository.findAll(pageable).map(OrderResponse::from);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        order.setStatus(OrderStatus.valueOf(status.toUpperCase()));
        return OrderResponse.from(orderRepository.save(order));
    }

    // ── Product Management ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ProductResponse> listAllProducts(String keyword, Pageable pageable) {
        Page<Product> page = (keyword != null && !keyword.isBlank())
                ? productRepository.search(keyword, pageable)
                : productRepository.findByActiveTrue(pageable);
        return page.map(p -> ProductResponse.from(p, 0.0, 0L));
    }
}
