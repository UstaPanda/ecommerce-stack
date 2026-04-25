package com.ecommerce.main.store;

import com.ecommerce.main.product.ProductRepository;
import com.ecommerce.main.review.ReviewRepository;
import com.ecommerce.main.storage.StorageService;
import com.ecommerce.main.user.User;
import com.ecommerce.main.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final StorageService storageService;

    public Store getById(Long id) {
        return storeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Store not found: " + id));
    }

    public Page<Store> getAll(Pageable pageable) {
        return storeRepository.findAll(pageable);
    }

    public Page<Store> getByStatus(StoreStatus status, Pageable pageable) {
        return storeRepository.findByStatus(status, pageable);
    }

    public List<Store> getMyStores(String email) {
        return storeRepository.findByOwnerEmail(email);
    }

    public Store create(String ownerEmail, StoreRequest request) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Store store = Store.builder()
                .owner(owner)
                .name(request.getName())
                .description(request.getDescription())
                .address(request.getAddress())
                .status(StoreStatus.PENDING)
                .build();
        return storeRepository.save(store);
    }

    public Store update(Long id, String requestorEmail, StoreRequest request) {
        Store store = getById(id);
        assertOwnerOrAdmin(store, requestorEmail);
        store.setName(request.getName());
        store.setDescription(request.getDescription());
        store.setAddress(request.getAddress());
        return storeRepository.save(store);
    }

    public Store changeStatus(Long id, StoreStatus status) {
        Store store = getById(id);
        store.setStatus(status);
        return storeRepository.save(store);
    }

    public Map<String, Object> getStats(Long id) {
        getById(id); // 404 check
        Double avgRating = reviewRepository.avgRatingByStore(id);
        long reviewCount = reviewRepository.countReviewsByStore(id);
        long productCount = productRepository.countByStoreId(id);
        double rounded = avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0;
        return Map.of("avgRating", rounded, "reviewCount", reviewCount, "productCount", productCount);
    }

    public Store uploadLogo(Long id, String requestorEmail, MultipartFile file) {
        Store store = getById(id);
        assertOwnerOrAdmin(store, requestorEmail);
        if (store.getLogoUrl() != null) {
            storageService.delete(store.getLogoUrl());
        }
        String url = storageService.upload(file, "stores");
        store.setLogoUrl(url);
        return storeRepository.save(store);
    }

    public void delete(Long id, String requestorEmail) {
        Store store = getById(id);
        assertOwnerOrAdmin(store, requestorEmail);
        storeRepository.delete(store);
    }

    private void assertOwnerOrAdmin(Store store, String requestorEmail) {
        User requestor = userRepository.findByEmail(requestorEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        boolean isAdmin = requestor.getRoleType().name().equals("ADMIN");
        boolean isOwner = store.getOwner().getEmail().equals(requestorEmail);
        if (!isAdmin && !isOwner) {
            throw new RuntimeException("Access denied");
        }
    }
}
