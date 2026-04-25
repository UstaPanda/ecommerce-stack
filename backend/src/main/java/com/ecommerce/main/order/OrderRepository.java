package com.ecommerce.main.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByUserEmail(String email, Pageable pageable);
    List<Order> findAllByUserEmail(String email);
    Page<Order> findByStoreId(Long storeId, Pageable pageable);
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    Page<Order> findByUserEmailAndStatus(String email, OrderStatus status, Pageable pageable);
    Page<Order> findByStoreIdAndStatus(Long storeId, OrderStatus status, Pageable pageable);

    long countByStatus(OrderStatus status);
    long countByStoreId(Long storeId);
    long countByStoreIdAndStatus(Long storeId, OrderStatus status);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.store.id = :storeId " +
           "AND (:from IS NULL OR o.createdAt >= :from) " +
           "AND (:to IS NULL OR o.createdAt <= :to)")
    long countByStoreIdAndDateRange(@Param("storeId") Long storeId, 
                                     @Param("from") LocalDateTime from, 
                                     @Param("to") LocalDateTime to);

    @Query("SELECT SUM(o.grandTotal) FROM Order o WHERE o.status NOT IN ('CANCELLED', 'RETURNED')")
    Double sumNetRevenue();

    @Query("SELECT SUM(o.grandTotal) FROM Order o WHERE o.status != 'CANCELLED'")
    Double sumTotalRevenue();

    @Query("SELECT SUM(o.grandTotal) FROM Order o WHERE o.store.id = :storeId AND o.status NOT IN ('CANCELLED', 'RETURNED')")
    Double sumRevenueByStore(@Param("storeId") Long storeId);

    // Revenue grouped by day for a store within a date range
    @Query("SELECT CAST(o.createdAt AS date), SUM(o.grandTotal) " +
           "FROM Order o WHERE o.store.id = :storeId " +
           "AND (:from IS NULL OR o.createdAt >= :from) " +
           "AND (:to IS NULL OR o.createdAt <= :to) " +
           "AND o.status NOT IN ('CANCELLED', 'RETURNED') " +
           "GROUP BY CAST(o.createdAt AS date) " +
           "ORDER BY CAST(o.createdAt AS date) ASC")
    List<Object[]> revenueByDayForStore(@Param("storeId") Long storeId,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);

    // Order status distribution for a store with date range
    @Query("SELECT o.status, COUNT(o) FROM Order o WHERE o.store.id = :storeId " +
           "AND (:from IS NULL OR o.createdAt >= :from) " +
           "AND (:to IS NULL OR o.createdAt <= :to) " +
           "GROUP BY o.status")
    List<Object[]> orderStatusDistributionByStoreAndDateRange(@Param("storeId") Long storeId,
                                                               @Param("from") LocalDateTime from,
                                                               @Param("to") LocalDateTime to);

    @Query("SELECT o.status, COUNT(o) FROM Order o WHERE o.store.id = :storeId GROUP BY o.status")
    List<Object[]> orderStatusDistributionByStore(@Param("storeId") Long storeId);

    @Query("SELECT SUM(o.grandTotal) FROM Order o WHERE o.store.id = :storeId " +
           "AND (:from IS NULL OR o.createdAt >= :from) " +
           "AND (:to IS NULL OR o.createdAt <= :to) " +
           "AND o.status NOT IN ('CANCELLED', 'RETURNED')")
    Double sumRevenueByStoreAndDateRange(@Param("storeId") Long storeId,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);

    // Individual: total order count + total spend
    @Query("SELECT COUNT(o), COALESCE(SUM(o.grandTotal), 0) FROM Order o " +
           "WHERE o.user.email = :email AND o.status NOT IN ('CANCELLED', 'RETURNED')")
    List<Object[]> spendSummaryByUser(@Param("email") String email);

    // Individual: order status distribution
    @Query("SELECT o.status, COUNT(o) FROM Order o WHERE o.user.email = :email GROUP BY o.status")
    List<Object[]> orderStatusDistributionByUser(@Param("email") String email);

    // Individual: monthly spending trend (YYYY-MM)
    @Query("SELECT FUNCTION('TO_CHAR', o.createdAt, 'YYYY-MM'), COALESCE(SUM(o.grandTotal), 0) " +
           "FROM Order o WHERE o.user.email = :email AND o.status NOT IN ('CANCELLED', 'RETURNED') " +
           "GROUP BY FUNCTION('TO_CHAR', o.createdAt, 'YYYY-MM') " +
           "ORDER BY FUNCTION('TO_CHAR', o.createdAt, 'YYYY-MM')")
    List<Object[]> monthlySpendByUser(@Param("email") String email);
}
