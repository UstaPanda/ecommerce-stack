package com.ecommerce.main.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SavedPaymentMethodRepository extends JpaRepository<SavedPaymentMethod, Long> {

    List<SavedPaymentMethod> findByUserEmailOrderByIsDefaultDescCreatedAtDesc(String email);

    Optional<SavedPaymentMethod> findByIdAndUserEmail(Long id, String email);

    @Modifying
    @Query("UPDATE SavedPaymentMethod s SET s.isDefault = false WHERE s.user.email = :email")
    void clearDefaultByUserEmail(@Param("email") String email);
}
