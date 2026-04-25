package com.ecommerce.main.customer;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;

    @GetMapping("/me")
    public ResponseEntity<CustomerProfile> getMyProfile(Authentication auth) {
        return ResponseEntity.ok(customerProfileService.getMyProfile(auth.getName()));
    }

    @PutMapping("/me")
    public ResponseEntity<CustomerProfile> updateMyProfile(Authentication auth,
                                                           @RequestBody CustomerProfileRequest request) {
        return ResponseEntity.ok(customerProfileService.updateMyProfile(auth.getName(), request));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CORPORATE')")
    public ResponseEntity<Page<CustomerProfile>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(customerProfileService.getAll(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CORPORATE')")
    public ResponseEntity<CustomerProfile> getById(@PathVariable Long id) {
        return ResponseEntity.ok(customerProfileService.getById(id));
    }
}
