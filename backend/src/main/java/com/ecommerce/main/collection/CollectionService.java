package com.ecommerce.main.collection;

import com.ecommerce.main.product.Product;
import com.ecommerce.main.product.ProductRepository;
import com.ecommerce.main.user.User;
import com.ecommerce.main.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CollectionService {

    private final CollectionRepository collectionRepository;
    private final CollectionItemRepository collectionItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<CollectionResponse> getMySummaries(String email) {
        return collectionRepository.findByUserEmailOrderByCreatedAtDesc(email)
                .stream().map(CollectionResponse::summary).toList();
    }

    @Transactional(readOnly = true)
    public CollectionResponse getDetail(String email, Long collectionId) {
        Collection c = collectionRepository.findByIdAndUserEmail(collectionId, email)
                .orElseThrow(() -> new RuntimeException("Koleksiyon bulunamadı."));
        return CollectionResponse.from(c);
    }

    public CollectionResponse create(String email, CollectionRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Collection c = Collection.builder()
                .user(user)
                .name(req.getName())
                .description(req.getDescription())
                .build();
        return CollectionResponse.from(collectionRepository.save(c));
    }

    public CollectionResponse update(String email, Long collectionId, CollectionRequest req) {
        Collection c = collectionRepository.findByIdAndUserEmail(collectionId, email)
                .orElseThrow(() -> new RuntimeException("Koleksiyon bulunamadı."));
        c.setName(req.getName());
        c.setDescription(req.getDescription());
        return CollectionResponse.from(collectionRepository.save(c));
    }

    public void delete(String email, Long collectionId) {
        Collection c = collectionRepository.findByIdAndUserEmail(collectionId, email)
                .orElseThrow(() -> new RuntimeException("Koleksiyon bulunamadı."));
        collectionRepository.delete(c);
    }

    public CollectionResponse addProduct(String email, Long collectionId, Long productId) {
        Collection c = collectionRepository.findByIdAndUserEmail(collectionId, email)
                .orElseThrow(() -> new RuntimeException("Koleksiyon bulunamadı."));
        if (collectionItemRepository.existsByCollectionIdAndProductId(collectionId, productId)) {
            throw new IllegalStateException("Ürün zaten koleksiyonda.");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        CollectionItem item = CollectionItem.builder()
                .collection(c)
                .product(product)
                .build();
        collectionItemRepository.save(item);
        return CollectionResponse.from(collectionRepository.findByIdAndUserEmail(collectionId, email).orElseThrow());
    }

    public CollectionResponse removeProduct(String email, Long collectionId, Long productId) {
        collectionRepository.findByIdAndUserEmail(collectionId, email)
                .orElseThrow(() -> new RuntimeException("Koleksiyon bulunamadı."));
        CollectionItem item = collectionItemRepository.findByCollectionIdAndProductId(collectionId, productId)
                .orElseThrow(() -> new RuntimeException("Ürün koleksiyonda bulunamadı."));
        collectionItemRepository.delete(item);
        return CollectionResponse.from(collectionRepository.findByIdAndUserEmail(collectionId, email).orElseThrow());
    }
}
