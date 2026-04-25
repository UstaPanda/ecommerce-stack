package com.ecommerce.main.admin;

import java.util.List;

public record AdminAnalyticsResponse(
        long totalUsers,
        long totalIndividualUsers,
        long totalCorporateUsers,
        long totalStores,
        long activeStores,
        long pendingStores,
        long totalOrders,
        long pendingOrders,
        long cancelledOrders,
        double totalRevenue,
        long totalProducts,
        long totalReviews,
        List<StoreComparisonEntry> storeComparison
) {
    public record StoreComparisonEntry(
            Long storeId,
            String storeName,
            long orderCount,
            double revenue
    ) {}
}
