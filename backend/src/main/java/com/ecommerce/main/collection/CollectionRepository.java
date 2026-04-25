package com.ecommerce.main.collection;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CollectionRepository extends JpaRepository<Collection, Long> {

    List<Collection> findByUserEmailOrderByCreatedAtDesc(String email);

    Optional<Collection> findByIdAndUserEmail(Long id, String email);
}
