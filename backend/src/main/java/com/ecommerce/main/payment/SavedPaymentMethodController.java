package com.ecommerce.main.payment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payment-methods")
@RequiredArgsConstructor
public class SavedPaymentMethodController {

    private final SavedPaymentMethodService service;

    @GetMapping
    public ResponseEntity<List<SavedPaymentMethodResponse>> getMyMethods(Authentication auth) {
        return ResponseEntity.ok(service.getMyMethods(auth.getName()));
    }

    @PostMapping
    public ResponseEntity<SavedPaymentMethodResponse> add(
            @Valid @RequestBody SavedPaymentMethodRequest req, Authentication auth) {
        return ResponseEntity.ok(service.add(auth.getName(), req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id, Authentication auth) {
        service.delete(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<SavedPaymentMethodResponse> setDefault(
            @PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(service.setDefault(auth.getName(), id));
    }
}
