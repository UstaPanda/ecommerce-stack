package com.ecommerce.main.collection;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CollectionItemRepository extends JpaRepository<CollectionItem, Long> {

    Optional<CollectionItem> findByCollectionIdAndProductId(Long collectionId, Long productId);

    boolean existsByCollectionIdAndProductId(Long collectionId, Long productId);
}
