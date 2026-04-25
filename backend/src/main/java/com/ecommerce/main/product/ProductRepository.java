package com.ecommerce.main.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByActiveTrue(Pageable pageable);
    Page<Product> findByStoreIdAndActiveTrue(Long storeId, Pageable pageable);
    Page<Product> findByCategoryIdAndActiveTrue(Long categoryId, Pageable pageable);
    Optional<Product> findBySku(String sku);
    List<Product> findByStoreIdAndStockQuantityLessThan(Long storeId, int threshold);

    long countByStoreId(Long storeId);

    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> search(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = """
            SELECT p.* FROM products p
            LEFT JOIN categories c ON p.category_id = c.id
            LEFT JOIN (
              SELECT product_id, AVG(star_rating) AS avg_rating
              FROM reviews GROUP BY product_id
            ) r_agg ON r_agg.product_id = p.id
            WHERE p.active = true
              AND (:keyword IS NULL OR
                   LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(COALESCE(c.name,'')) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:categoryId IS NULL OR p.category_id = :categoryId OR c.parent_id = :categoryId)
              AND (:minPrice IS NULL OR p.unit_price >= :minPrice)
              AND (:maxPrice IS NULL OR p.unit_price <= :maxPrice)
              AND (:minRating IS NULL OR COALESCE(r_agg.avg_rating, 0) >= :minRating)
            ORDER BY
              CASE WHEN :sortOrder = 'newest' THEN p.created_at END DESC NULLS LAST,
              CASE WHEN :sortOrder = 'oldest' THEN p.created_at END ASC NULLS LAST,
              p.id DESC
            """,
           countQuery = """
            SELECT COUNT(*) FROM products p
            LEFT JOIN categories c ON p.category_id = c.id
            LEFT JOIN (
              SELECT product_id, AVG(star_rating) AS avg_rating
              FROM reviews GROUP BY product_id
            ) r_agg ON r_agg.product_id = p.id
            WHERE p.active = true
              AND (:keyword IS NULL OR
                   LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(COALESCE(c.name,'')) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:categoryId IS NULL OR p.category_id = :categoryId OR c.parent_id = :categoryId)
              AND (:minPrice IS NULL OR p.unit_price >= :minPrice)
              AND (:maxPrice IS NULL OR p.unit_price <= :maxPrice)
              AND (:minRating IS NULL OR COALESCE(r_agg.avg_rating, 0) >= :minRating)
            """,
           nativeQuery = true)
    Page<Product> filter(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("minRating") Double minRating,
            @Param("sortOrder") String sortOrder,
            Pageable pageable);

    @Query(value = """
            SELECT name FROM (
              SELECT p.name,
                CASE WHEN LOWER(p.name) LIKE LOWER(:keyword) || '%' THEN 0 ELSE 1 END AS prefix_rank,
                word_similarity(LOWER(:keyword), LOWER(p.name)) AS sim
              FROM products p
              LEFT JOIN categories c ON p.category_id = c.id
              WHERE p.active = true
                AND (
                  p.name ILIKE '%' || :keyword || '%'
                  OR c.name ILIKE '%' || :keyword || '%'
                  OR word_similarity(LOWER(:keyword), LOWER(p.name)) > 0.15
                  OR word_similarity(LOWER(:keyword), LOWER(COALESCE(c.name,''))) > 0.2
                )
            ) ranked
            ORDER BY prefix_rank, sim DESC, name
            LIMIT 8
            """, nativeQuery = true)
    List<String> findNameSuggestions(@Param("keyword") String keyword);

    // Popular products — highest stock (proxy for popularity until order data exists)
    @Query("SELECT p FROM Product p WHERE p.active = true ORDER BY p.stockQuantity DESC")
    List<Product> findPopular(Pageable pageable);

    // Product count per category
    @Query("SELECT p.category.id, COUNT(p) FROM Product p WHERE p.active = true AND p.category IS NOT NULL GROUP BY p.category.id")
    List<Object[]> countByCategory();

    // Top selling products for a store: product id, name, total qty sold, total revenue
    @Query("SELECT p.id, p.name, SUM(oi.quantity), SUM(oi.quantity * oi.unitPrice) " +
           "FROM OrderItem oi JOIN oi.product p " +
           "WHERE p.store.id = :storeId AND oi.order.status != 'CANCELLED' " +
           "GROUP BY p.id, p.name ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> topSellingByStore(@Param("storeId") Long storeId, Pageable pageable);

    @Query("SELECT p.id, p.name, SUM(oi.quantity), SUM(oi.quantity * oi.unitPrice) " +
           "FROM OrderItem oi JOIN oi.product p " +
           "WHERE p.store.id = :storeId AND oi.order.status != 'CANCELLED' " +
           "AND oi.order.createdAt >= :from AND oi.order.createdAt <= :to " +
           "GROUP BY p.id, p.name ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> topSellingByStoreAndDateRange(@Param("storeId") Long storeId, 
                                                  @Param("from") java.time.LocalDateTime from, 
                                                  @Param("to") java.time.LocalDateTime to, 
                                                  Pageable pageable);
}
