package com.ecommerce.main.address;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class UserAddressController {

    private final UserAddressService addressService;

    @GetMapping
    public ResponseEntity<List<UserAddressResponse>> getMyAddresses(Authentication auth) {
        return ResponseEntity.ok(addressService.getMyAddresses(auth.getName()));
    }

    @PostMapping
    public ResponseEntity<UserAddressResponse> create(Authentication auth,
                                                      @RequestBody UserAddressRequest req) {
        return ResponseEntity.ok(addressService.create(auth.getName(), req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserAddressResponse> update(Authentication auth,
                                                      @PathVariable Long id,
                                                      @RequestBody UserAddressRequest req) {
        return ResponseEntity.ok(addressService.update(auth.getName(), id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication auth, @PathVariable Long id) {
        addressService.delete(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<UserAddressResponse> setDefault(Authentication auth,
                                                          @PathVariable Long id) {
        return ResponseEntity.ok(addressService.setDefault(auth.getName(), id));
    }
}
