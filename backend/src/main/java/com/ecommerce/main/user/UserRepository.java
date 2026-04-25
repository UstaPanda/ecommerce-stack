package com.ecommerce.main.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByVerificationCode(String code);

    Page<User> findByRoleType(Role roleType, Pageable pageable);
    Page<User> findAll(Pageable pageable);

    long countByRoleType(Role roleType);

    @Query("SELECT u FROM User u WHERE " +
           "(:role IS NULL OR u.roleType = :role) AND " +
           "(:keyword IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<User> searchUsers(@Param("role") Role role,
                           @Param("keyword") String keyword,
                           Pageable pageable);
}
