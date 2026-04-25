package com.ecommerce.main.address;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    List<UserAddress> findByUserEmailOrderByIsDefaultDescCreatedAtDesc(String email);

    @Modifying
    @Query("UPDATE UserAddress a SET a.isDefault = false WHERE a.user.email = :email")
    void clearDefaultByUserEmail(@Param("email") String email);
}
