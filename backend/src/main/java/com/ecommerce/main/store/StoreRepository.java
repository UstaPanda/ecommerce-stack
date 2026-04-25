package com.ecommerce.main.store;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StoreRepository extends JpaRepository<Store, Long> {
    List<Store> findByOwnerEmail(String email);
    Page<Store> findByStatus(StoreStatus status, Pageable pageable);
    List<Store> findByOwnerEmailAndStatus(String email, StoreStatus status);

    long countByStatus(StoreStatus status);

    @Query("SELECT s.id, s.name, COUNT(o.id), COALESCE(SUM(o.grandTotal), 0) " +
           "FROM Store s LEFT JOIN Order o ON o.store.id = s.id AND o.status NOT IN ('CANCELLED', 'RETURNED') " +
           "GROUP BY s.id, s.name ORDER BY SUM(o.grandTotal) DESC NULLS LAST")
    List<Object[]> storeRevenueComparison();
}
