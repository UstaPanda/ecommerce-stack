package com.ecommerce.main.product;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        Double unitPrice,
        Integer stockQuantity,
        String productImportance,
        String imageUrl,
        Boolean active,
        StoreInfo store,
        CategoryInfo category,
        Double avgRating,
        Long reviewCount
) {
    public record StoreInfo(Long id, String name) {}
    public record CategoryInfo(Long id, String name) {}

    public static ProductResponse from(Product p, Double avgRating, Long reviewCount) {
        StoreInfo store = new StoreInfo(p.getStore().getId(), p.getStore().getName());
        CategoryInfo category = p.getCategory() != null
                ? new CategoryInfo(p.getCategory().getId(), p.getCategory().getName())
                : null;
        double rounded = avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0;
        return new ProductResponse(
                p.getId(), p.getSku(), p.getName(), p.getDescription(),
                p.getUnitPrice(), p.getStockQuantity(), p.getProductImportance(),
                p.getImageUrl(), p.getActive(), store, category, rounded, reviewCount != null ? reviewCount : 0L
        );
    }
}
