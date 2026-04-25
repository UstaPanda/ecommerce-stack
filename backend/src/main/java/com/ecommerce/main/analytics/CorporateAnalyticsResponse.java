package com.ecommerce.main.analytics;

import java.util.List;

public record CorporateAnalyticsResponse(
        Long storeId,
        String storeName,
        long totalOrders,
        long pendingOrders,
        long cancelledOrders,
        double totalRevenue,
        long totalProducts,
        long lowStockProducts,
        double avgReviewRating,
        List<RevenueByDay> revenueByDay,
        List<TopProduct> topProducts,
        List<OrderStatusCount> orderStatusDistribution,
        List<MembershipCount> customerSegmentation
) {
    public record RevenueByDay(String date, double revenue) {}

    public record TopProduct(
            Long productId,
            String productName,
            long totalQuantitySold,
            double totalRevenue
    ) {}

    public record OrderStatusCount(String status, long count) {}

    public record MembershipCount(String membershipType, long count) {}
}
