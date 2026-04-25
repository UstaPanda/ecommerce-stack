package com.ecommerce.main.analytics;

import com.ecommerce.main.order.OrderItemRepository;
import com.ecommerce.main.order.OrderRepository;
import com.ecommerce.main.review.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IndividualAnalyticsService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public IndividualAnalyticsResponse getMyAnalytics(String email) {

        // Total orders + total spend
        List<Object[]> summaryList = orderRepository.spendSummaryByUser(email);
        long totalOrders = 0L;
        double totalSpend = 0.0;
        
        if (summaryList != null && !summaryList.isEmpty() && summaryList.get(0) != null) {
            Object[] summary = summaryList.get(0);
            totalOrders = (summary.length > 0 && summary[0] != null) ? ((Number) summary[0]).longValue() : 0L;
            totalSpend = (summary.length > 1 && summary[1] != null) ? ((Number) summary[1]).doubleValue() : 0.0;
        }
        
        double avgOrderValue = totalOrders > 0 ? totalSpend / totalOrders : 0.0;

        // Review count
        long totalReviews = reviewRepository.findByUserEmail(email,
                PageRequest.of(0, 1)).getTotalElements();

        // Order status distribution
        List<IndividualAnalyticsResponse.OrderStatusCount> statusDist =
                orderRepository.orderStatusDistributionByUser(email).stream()
                        .filter(row -> row != null && row.length > 1 && row[0] != null && row[1] != null)
                        .map(row -> new IndividualAnalyticsResponse.OrderStatusCount(
                                row[0].toString(),
                                ((Number) row[1]).longValue()
                        ))
                        .toList();

        // Monthly spend trend
        List<IndividualAnalyticsResponse.MonthlySpend> monthlyTrend =
                orderRepository.monthlySpendByUser(email).stream()
                        .filter(row -> row != null && row.length > 1 && row[0] != null && row[1] != null)
                        .map(row -> new IndividualAnalyticsResponse.MonthlySpend(
                                row[0].toString(),
                                ((Number) row[1]).doubleValue()
                        ))
                        .toList();

        // Spend by category
        List<IndividualAnalyticsResponse.CategorySpend> categorySpend =
                orderItemRepository.spendByCategoryForUser(email).stream()
                        .filter(row -> row != null && row.length > 1 && row[0] != null && row[1] != null)
                        .map(row -> new IndividualAnalyticsResponse.CategorySpend(
                                row[0].toString(),
                                ((Number) row[1]).doubleValue()
                        ))
                        .toList();

        // Top 5 most purchased products
        List<IndividualAnalyticsResponse.TopProduct> topProducts =
                orderItemRepository.topPurchasedByUser(email, PageRequest.of(0, 5)).stream()
                        .filter(row -> row != null && row.length > 3 && row[0] != null && row[1] != null && row[2] != null && row[3] != null)
                        .map(row -> new IndividualAnalyticsResponse.TopProduct(
                                ((Number) row[0]).longValue(),
                                row[1].toString(),
                                ((Number) row[2]).longValue(),
                                ((Number) row[3]).doubleValue()
                        ))
                        .toList();

        return new IndividualAnalyticsResponse(
                totalOrders, totalSpend, avgOrderValue, totalReviews,
                statusDist, monthlyTrend, categorySpend, topProducts
        );
    }
}
