package com.ecommerce.main.order;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);

    @Query("SELECT oi.product.id, oi.product.name, SUM(oi.quantity) as totalQty, SUM(oi.quantity * oi.unitPrice) as revenue " +
           "FROM OrderItem oi WHERE oi.order.store.id = :storeId " +
           "GROUP BY oi.product.id, oi.product.name ORDER BY revenue DESC")
    List<Object[]> findTopProductsByStoreRevenue(@Param("storeId") Long storeId);

    // Individual: spending grouped by category
    @Query("SELECT COALESCE(oi.product.category.name, 'Uncategorized'), " +
           "SUM(oi.quantity * oi.unitPrice) " +
           "FROM OrderItem oi WHERE oi.order.user.email = :email " +
           "AND oi.order.status != 'CANCELLED' " +
           "GROUP BY oi.product.category.name ORDER BY SUM(oi.quantity * oi.unitPrice) DESC")
    List<Object[]> spendByCategoryForUser(@Param("email") String email);

    // Individual: most purchased products
    @Query("SELECT oi.product.id, oi.product.name, SUM(oi.quantity), SUM(oi.quantity * oi.unitPrice) " +
           "FROM OrderItem oi WHERE oi.order.user.email = :email " +
           "AND oi.order.status != 'CANCELLED' " +
           "GROUP BY oi.product.id, oi.product.name ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> topPurchasedByUser(@Param("email") String email, Pageable pageable);
}
