package com.ecommerce.main.order;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** Individual: kendi siparişlerini listeler */
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('ROLE_INDIVIDUAL')")
    public ResponseEntity<Page<OrderResponse>> getMyOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(orderService.getMyOrders(auth.getName(), status, pageable));
    }

    /** Corporate/Admin: store'a ait siparişleri listeler */
    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasAnyAuthority('ROLE_CORPORATE', 'ROLE_ADMIN')")
    public ResponseEntity<Page<OrderResponse>> getStoreOrders(
            @PathVariable Long storeId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(orderService.getStoreOrders(storeId, auth.getName(), status, pageable));
    }

    /** Sipariş detayı — individual (kendi), corporate (kendi store'u), admin (hepsi) */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(orderService.getById(id, auth.getName()));
    }

    /** Individual: sipariş oluşturur */
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_INDIVIDUAL')")
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody OrderRequest request,
            Authentication auth) {
        return ResponseEntity.ok(orderService.createOrder(auth.getName(), request));
    }

    /** Individual: PENDING siparişini iptal eder */
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('ROLE_INDIVIDUAL')")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(orderService.cancelOrder(id, auth.getName()));
    }

    /** Individual: kendi sipariş geçmişini CSV olarak indirir */
    @GetMapping("/my/export")
    @PreAuthorize("hasAuthority('ROLE_INDIVIDUAL')")
    public ResponseEntity<byte[]> exportMyOrders(Authentication auth) {
        byte[] csv = orderService.exportMyOrdersCsv(auth.getName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"orders.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    /** Corporate/Admin: sipariş durumunu günceller */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_CORPORATE', 'ROLE_ADMIN')")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status,
            Authentication auth) {
        return ResponseEntity.ok(orderService.updateStatus(id, status, auth.getName()));
    }
}
