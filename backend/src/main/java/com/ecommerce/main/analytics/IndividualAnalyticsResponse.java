package com.ecommerce.main.analytics;

import java.util.List;

public record IndividualAnalyticsResponse(
        long totalOrders,
        double totalSpend,
        double avgOrderValue,
        long totalReviews,
        List<OrderStatusCount> orderStatusDistribution,
        List<MonthlySpend> monthlySpendTrend,
        List<CategorySpend> spendByCategory,
        List<TopProduct> topPurchasedProducts
) {
    public record OrderStatusCount(String status, long count) {}

    public record MonthlySpend(String month, double amount) {}

    public record CategorySpend(String category, double amount) {}

    public record TopProduct(
            Long productId,
            String productName,
            long totalQuantity,
            double totalSpend
    ) {}
}
