package com.ecommerce.main.store;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    @GetMapping
    public ResponseEntity<Page<Store>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(storeService.getAll(pageable));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Page<Store>> getByStatus(
            @PathVariable StoreStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(storeService.getByStatus(status, pageable));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('ROLE_CORPORATE')")
    public ResponseEntity<List<Store>> getMyStores(Authentication auth) {
        return ResponseEntity.ok(storeService.getMyStores(auth.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Store> getById(@PathVariable Long id) {
        return ResponseEntity.ok(storeService.getById(id));
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<Map<String, Object>> getStats(@PathVariable Long id) {
        return ResponseEntity.ok(storeService.getStats(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_CORPORATE')")
    public ResponseEntity<Store> create(@Valid @RequestBody StoreRequest request, Authentication auth) {
        return ResponseEntity.ok(storeService.create(auth.getName(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_CORPORATE', 'ROLE_ADMIN')")
    public ResponseEntity<Store> update(@PathVariable Long id,
                                        @Valid @RequestBody StoreRequest request,
                                        Authentication auth) {
        return ResponseEntity.ok(storeService.update(id, auth.getName(), request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Store> changeStatus(@PathVariable Long id,
                                               @RequestParam StoreStatus status) {
        return ResponseEntity.ok(storeService.changeStatus(id, status));
    }

    @PostMapping("/{id}/logo")
    @PreAuthorize("hasAnyAuthority('ROLE_CORPORATE', 'ROLE_ADMIN')")
    public ResponseEntity<Store> uploadLogo(@PathVariable Long id,
                                            @RequestParam("file") MultipartFile file,
                                            Authentication auth) {
        return ResponseEntity.ok(storeService.uploadLogo(id, auth.getName(), file));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_CORPORATE', 'ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        storeService.delete(id, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
