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

        long totalOrders     = orderRepository.countByStoreIdAndDateRange(storeId, from, to);
        long pendingOrders   = orderRepository.orderStatusDistributionByStoreAndDateRange(storeId, from, to).stream()
                .filter(r -> r[0].toString().equals("PENDING")).map(r -> ((Number) r[1]).longValue()).findFirst().orElse(0L);
        long cancelledOrders = orderRepository.orderStatusDistributionByStoreAndDateRange(storeId, from, to).stream()
                .filter(r -> r[0].toString().equals("CANCELLED")).map(r -> ((Number) r[1]).longValue()).findFirst().orElse(0L);
        
        Double rev           = orderRepository.sumRevenueByStoreAndDateRange(storeId, from, to);
        double totalRevenue  = rev != null ? rev : 0.0;
        
        long totalProducts   = productRepository.countByStoreId(storeId);
        long lowStock        = productRepository.findByStoreIdAndStockQuantityLessThan(storeId, 10).size();
        Double avgRating     = reviewRepository.avgRatingByStore(storeId);

        // Revenue by day
        List<CorporateAnalyticsResponse.RevenueByDay> revenueByDay =
                orderRepository.revenueByDayForStore(storeId, from, to).stream()
                        .map(row -> {
                            String dateStr = (row[0] != null) ? row[0].toString() : "Undated";
                            double amount = (row[1] != null) ? ((Number) row[1]).doubleValue() : 0.0;
                            return new CorporateAnalyticsResponse.RevenueByDay(dateStr, amount);
                        })
                        .toList();

        // Top 10 products
        List<CorporateAnalyticsResponse.TopProduct> topProducts =
                productRepository.topSellingByStore(storeId, PageRequest.of(0, 10)).stream()
                        .map(row -> new CorporateAnalyticsResponse.TopProduct(
                                ((Number) row[0]).longValue(),
                                (String) row[1],
                                ((Number) row[2]).longValue(),
                                ((Number) row[3]).doubleValue()
                        ))
                        .toList();

        // Order status distribution
        List<CorporateAnalyticsResponse.OrderStatusCount> statusDist =
                orderRepository.orderStatusDistributionByStoreAndDateRange(storeId, from, to).stream()
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
