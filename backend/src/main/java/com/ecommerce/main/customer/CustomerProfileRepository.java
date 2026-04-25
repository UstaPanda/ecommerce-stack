package com.ecommerce.main.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {
    Optional<CustomerProfile> findByUserEmail(String email);
    Optional<CustomerProfile> findByUserId(Long userId);

    // Customers who ordered from a specific store
    @Query("SELECT cp FROM CustomerProfile cp WHERE cp.user.id IN " +
           "(SELECT DISTINCT o.user.id FROM Order o WHERE o.store.id = :storeId)")
    Page<CustomerProfile> findByStoreId(@Param("storeId") Long storeId, Pageable pageable);

    // Membership distribution for a store's customers
    @Query("SELECT cp.membershipType, COUNT(cp) FROM CustomerProfile cp WHERE cp.user.id IN " +
           "(SELECT DISTINCT o.user.id FROM Order o WHERE o.store.id = :storeId) " +
           "GROUP BY cp.membershipType")
    List<Object[]> membershipDistributionByStore(@Param("storeId") Long storeId);
}
