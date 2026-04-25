package com.ecommerce.main.analytics;

import com.ecommerce.main.customer.CustomerProfileRepository;
import com.ecommerce.main.order.OrderRepository;
import com.ecommerce.main.order.OrderStatus;
import com.ecommerce.main.product.ProductRepository;
import com.ecommerce.main.review.ReviewRepository;
import com.ecommerce.main.store.Store;
import com.ecommerce.main.store.StoreRepository;
import com.ecommerce.main.user.Role;
import com.ecommerce.main.user.User;
import com.ecommerce.main.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CorporateAnalyticsService {

    private final StoreRepository storeRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Transactional(readOnly = true)
    public CorporateAnalyticsResponse getStoreAnalytics(Long storeId, String requestorEmail,
                                                         LocalDateTime from, LocalDateTime to) {
        assertStoreAccess(storeId, requestorEmail);

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));

        long totalOrders;
        long pendingOrders;
        long cancelledOrders;
        double totalRevenue;
        List<Object[]> rawRevenueByDay;
        List<Object[]> rawStatusDist;

        if (from == null) {
            // LIFETIME MODE: Show everything including undated orders
            totalOrders = orderRepository.countByStoreId(storeId);
            
            rawStatusDist = orderRepository.orderStatusDistributionByStore(storeId);
            pendingOrders = rawStatusDist.stream()
                .filter(r -> r[0].toString().equals("PENDING")).map(r -> ((Number) r[1]).longValue()).findFirst().orElse(0L);
            cancelledOrders = rawStatusDist.stream()
                .filter(r -> r[0].toString().equals("CANCELLED")).map(r -> ((Number) r[1]).longValue()).findFirst().orElse(0L);
            
            Double rev = orderRepository.sumRevenueByStore(storeId);
            totalRevenue = rev != null ? rev : 0.0;
            
            rawRevenueByDay = orderRepository.revenueByDayForStoreLifetime(storeId);
        } else {
            // FILTERED MODE: Strict date range
            LocalDateTime effectiveTo = to != null ? to : LocalDateTime.now();
            totalOrders = orderRepository.countByStoreIdAndDateRange(storeId, from, effectiveTo);
            
            rawStatusDist = orderRepository.orderStatusDistributionByStoreAndDateRange(storeId, from, effectiveTo);
            pendingOrders = rawStatusDist.stream()
                .filter(r -> r[0].toString().equals("PENDING")).map(r -> ((Number) r[1]).longValue()).findFirst().orElse(0L);
            cancelledOrders = rawStatusDist.stream()
                .filter(r -> r[0].toString().equals("CANCELLED")).map(r -> ((Number) r[1]).longValue()).findFirst().orElse(0L);
            
            Double rev = orderRepository.sumRevenueByStoreAndDateRange(storeId, from, effectiveTo);
            totalRevenue = rev != null ? rev : 0.0;
            
            rawRevenueByDay = orderRepository.revenueByDayForStore(storeId, from, effectiveTo);
        }

        long totalProducts   = productRepository.countByStoreId(storeId);
        long lowStock        = productRepository.findByStoreIdAndStockQuantityLessThan(storeId, 10).size();
        Double avgRating     = reviewRepository.avgRatingByStore(storeId);

        // Map Revenue
        List<CorporateAnalyticsResponse.RevenueByDay> revenueByDay = rawRevenueByDay.stream()
                .map(row -> new CorporateAnalyticsResponse.RevenueByDay(
                        row[0] != null ? row[0].toString() : "Undated",
                        ((Number) row[1]).doubleValue()
                ))
                .toList();

        // Top 10 products (All-time selling)
        List<CorporateAnalyticsResponse.TopProduct> topProducts =
                productRepository.topSellingByStore(storeId, PageRequest.of(0, 10)).stream()
                        .map(row -> new CorporateAnalyticsResponse.TopProduct(
                                ((Number) row[0]).longValue(),
                                (String) row[1],
                                ((Number) row[2]).longValue(),
                                ((Number) row[3]).doubleValue()
                        ))
                        .toList();

        // Map Status Distribution
        List<CorporateAnalyticsResponse.OrderStatusCount> statusDist = rawStatusDist.stream()
                .map(row -> new CorporateAnalyticsResponse.OrderStatusCount(
                        row[0].toString(),
                        ((Number) row[1]).longValue()
                ))
                .toList();

        // Customer segmentation by membership
        List<CorporateAnalyticsResponse.MembershipCount> segmentation =
                customerProfileRepository.membershipDistributionByStore(storeId).stream()
                        .map(row -> new CorporateAnalyticsResponse.MembershipCount(
                                row[0].toString(),
                                ((Number) row[1]).longValue()
                        ))
                        .toList();

        return new CorporateAnalyticsResponse(
                storeId,
                store.getName(),
                totalOrders, pendingOrders, cancelledOrders,
                totalRevenue, totalProducts, lowStock,
                avgRating != null ? avgRating : 0.0,
                revenueByDay, topProducts, statusDist, segmentation
        );
    }

    private void assertStoreAccess(Long storeId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getRoleType() == Role.ADMIN) return;
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));
        if (!store.getOwner().getEmail().equals(email)) {
            throw new IllegalStateException("Access denied");
        }
    }
}
