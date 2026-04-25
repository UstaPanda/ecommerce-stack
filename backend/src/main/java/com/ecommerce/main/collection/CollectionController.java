package com.ecommerce.main.collection;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    @GetMapping
    public ResponseEntity<List<CollectionResponse>> getMySummaries(Authentication auth) {
        return ResponseEntity.ok(collectionService.getMySummaries(auth.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CollectionResponse> getDetail(
            @PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(collectionService.getDetail(auth.getName(), id));
    }

    @PostMapping
    public ResponseEntity<CollectionResponse> create(
            @Valid @RequestBody CollectionRequest req, Authentication auth) {
        return ResponseEntity.ok(collectionService.create(auth.getName(), req));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CollectionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CollectionRequest req,
            Authentication auth) {
        return ResponseEntity.ok(collectionService.update(auth.getName(), id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id, Authentication auth) {
        collectionService.delete(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/items/{productId}")
    public ResponseEntity<CollectionResponse> addProduct(
            @PathVariable Long id,
            @PathVariable Long productId,
            Authentication auth) {
        return ResponseEntity.ok(collectionService.addProduct(auth.getName(), id, productId));
    }

    @DeleteMapping("/{id}/items/{productId}")
    public ResponseEntity<CollectionResponse> removeProduct(
            @PathVariable Long id,
            @PathVariable Long productId,
            Authentication auth) {
        return ResponseEntity.ok(collectionService.removeProduct(auth.getName(), id, productId));
    }
}
