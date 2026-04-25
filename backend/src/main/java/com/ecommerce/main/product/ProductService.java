package com.ecommerce.main.product;

import com.ecommerce.main.category.Category;
import com.ecommerce.main.category.CategoryRepository;
import com.ecommerce.main.review.ReviewRepository;
import com.ecommerce.main.storage.StorageService;
import com.ecommerce.main.store.Store;
import com.ecommerce.main.store.StoreRepository;
import com.ecommerce.main.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final StorageService storageService;

    public Page<ProductResponse> getAll(Pageable pageable) {
        return enrichPage(productRepository.findByActiveTrue(pageable));
    }

    public Page<ProductResponse> getByStore(Long storeId, Pageable pageable) {
        return enrichPage(productRepository.findByStoreIdAndActiveTrue(storeId, pageable));
    }

    public Page<ProductResponse> getByCategory(Long categoryId, Pageable pageable) {
        return enrichPage(productRepository.findByCategoryIdAndActiveTrue(categoryId, pageable));
    }

    public Page<ProductResponse> search(String keyword, Pageable pageable) {
        return enrichPage(productRepository.search(keyword, pageable));
    }

    public Page<ProductResponse> filter(String keyword, Long categoryId, Double minPrice, Double maxPrice, Double minRating, String sortOrder, Pageable pageable) {
        Page<Product> page = productRepository.filter(
                (keyword != null && keyword.isBlank()) ? null : keyword,
                categoryId, minPrice, maxPrice, minRating,
                (sortOrder != null && !sortOrder.isBlank()) ? sortOrder : null,
                pageable);
        return enrichPage(page);
    }

    public List<ProductResponse> getPopular(int limit) {
        List<Product> products = productRepository.findPopular(
                org.springframework.data.domain.PageRequest.of(0, limit));
        return enrichList(products);
    }

    public java.util.Map<Long, Long> getCountByCategory() {
        java.util.Map<Long, Long> result = new java.util.HashMap<>();
        productRepository.countByCategory().forEach(row ->
                result.put((Long) row[0], (Long) row[1]));
        return result;
    }

    public List<String> getSuggestions(String keyword) {
        return productRepository.findNameSuggestions(keyword);
    }

    public ProductResponse getById(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
        Double avg = reviewRepository.avgRatingByProduct(id);
        long count = reviewRepository.countByProductId(id);
        return ProductResponse.from(p, avg, count);
    }

    public List<ProductResponse> getLowStock(Long storeId, int threshold) {
        return enrichList(productRepository.findByStoreIdAndStockQuantityLessThan(storeId, threshold));
    }

    @Transactional
    public ProductResponse create(Long storeId, String requestorEmail, ProductRequest request) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found: " + storeId));
        assertStoreOwner(store, requestorEmail);

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
        }

        Product product = Product.builder()
                .store(store)
                .category(category)
                .sku(request.getSku())
                .name(request.getName())
                .description(request.getDescription())
                .unitPrice(request.getUnitPrice())
                .stockQuantity(request.getStockQuantity())
                .productImportance(request.getProductImportance())
                .imageUrl(request.getImageUrl())
                .build();
        return ProductResponse.from(productRepository.save(product), null, 0L);
    }

    @Transactional
    public ProductResponse update(Long id, String requestorEmail, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
        assertStoreOwner(product.getStore(), requestorEmail);

        product.setName(request.getName());
        product.setSku(request.getSku());
        product.setDescription(request.getDescription());
        product.setUnitPrice(request.getUnitPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setProductImportance(request.getProductImportance());
        if (request.getImageUrl() != null) {
            product.setImageUrl(request.getImageUrl());
        }

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            product.setCategory(category);
        }
        Double avg = reviewRepository.avgRatingByProduct(id);
        long count = reviewRepository.countByProductId(id);
        return ProductResponse.from(productRepository.save(product), avg, count);
    }

    @Transactional
    public ProductResponse uploadImage(Long id, String requestorEmail, MultipartFile file) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
        assertStoreOwner(product.getStore(), requestorEmail);
        if (product.getImageUrl() != null) {
            storageService.delete(product.getImageUrl());
        }
        String url = storageService.upload(file, "products");
        product.setImageUrl(url);
        productRepository.save(product);
        Double avg = reviewRepository.avgRatingByProduct(id);
        long count = reviewRepository.countByProductId(id);
        return ProductResponse.from(product, avg, count);
    }

    @Transactional
    public void delete(Long id, String requestorEmail) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
        assertStoreOwner(product.getStore(), requestorEmail);
        product.setActive(false);
        productRepository.save(product);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Page<ProductResponse> enrichPage(Page<Product> page) {
        List<Product> content = page.getContent();
        if (content.isEmpty()) {
            return new PageImpl<>(List.of(), page.getPageable(), page.getTotalElements());
        }
        List<Long> ids = content.stream().map(Product::getId).toList();
        Map<Long, Object[]> ratingMap = buildRatingMap(ids);
        List<ProductResponse> enriched = content.stream()
                .map(p -> toResponse(p, ratingMap))
                .toList();
        return new PageImpl<>(enriched, page.getPageable(), page.getTotalElements());
    }

    private List<ProductResponse> enrichList(List<Product> products) {
        if (products.isEmpty()) return List.of();
        List<Long> ids = products.stream().map(Product::getId).toList();
        Map<Long, Object[]> ratingMap = buildRatingMap(ids);
        return products.stream().map(p -> toResponse(p, ratingMap)).toList();
    }

    private Map<Long, Object[]> buildRatingMap(List<Long> ids) {
        Map<Long, Object[]> map = new HashMap<>();
        reviewRepository.avgAndCountByProductIds(ids).forEach(row -> map.put(((Number) row[0]).longValue(), row));
        return map;
    }

    private ProductResponse toResponse(Product p, Map<Long, Object[]> ratingMap) {
        Object[] row = ratingMap.get(p.getId());
        Double avg = row != null && row[1] != null ? ((Number) row[1]).doubleValue() : null;
        Long count = row != null ? ((Number) row[2]).longValue() : 0L;
        return ProductResponse.from(p, avg, count);
    }

    private void assertStoreOwner(Store store, String requestorEmail) {
        boolean isAdmin = userRepository.findByEmail(requestorEmail)
                .map(u -> u.getRoleType().name().equals("ADMIN"))
                .orElse(false);
        if (!isAdmin && !store.getOwner().getEmail().equals(requestorEmail)) {
            throw new RuntimeException("Access denied");
        }
    }
}
