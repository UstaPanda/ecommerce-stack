package com.ecommerce.main.admin;

import com.ecommerce.main.audit.AuditEventType;
import com.ecommerce.main.audit.AuditLog;
import com.ecommerce.main.audit.AuditLogRepository;
import com.ecommerce.main.coupon.CouponDto;
import com.ecommerce.main.order.OrderResponse;
import com.ecommerce.main.product.ProductResponse;
import com.ecommerce.main.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final AuditLogRepository auditLogRepository;

    // ── Analytics ────────────────────────────────────────────────────────────

    @GetMapping("/analytics")
    public ResponseEntity<AdminAnalyticsResponse> getAnalytics() {
        return ResponseEntity.ok(adminService.getPlatformAnalytics());
    }

    // ── User Management ──────────────────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<Page<UserResponse>> listUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(adminService.listUsers(role, keyword, pageable));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUserById(id));
    }

    @PatchMapping("/users/{id}/suspend")
    public ResponseEntity<UserResponse> suspendUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.suspendUser(id));
    }

    @PatchMapping("/users/{id}/unsuspend")
    public ResponseEntity<UserResponse> unsuspendUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.unsuspendUser(id));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<UserResponse> changeRole(
            @PathVariable Long id,
            @RequestParam Role role) {
        return ResponseEntity.ok(adminService.changeRole(id, role));
    }

    // ── Store Management ─────────────────────────────────────────────────────

    @GetMapping("/stores")
    public ResponseEntity<Page<AdminStoreDto>> listStores(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(adminService.listStores(keyword, pageable));
    }

    @PutMapping("/stores/{id}/status")
    public ResponseEntity<AdminStoreDto> updateStoreStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String status = (String) body.get("status");
        return ResponseEntity.ok(adminService.updateStoreStatus(id, com.ecommerce.main.store.StoreStatus.valueOf(status)));
    }

    // ── Platform Settings ────────────────────────────────────────────────────

    @GetMapping("/settings")
    public ResponseEntity<PlatformSettings> getSettings() {
        return ResponseEntity.ok(adminService.getSettings());
    }

    @PutMapping("/settings")
    public ResponseEntity<PlatformSettings> updateSettings(@RequestBody PlatformSettings settings) {
        return ResponseEntity.ok(adminService.updateSettings(settings));
    }

    // ── Audit Logs ───────────────────────────────────────────────────────────

    @GetMapping("/audit-logs")
    public ResponseEntity<Page<AuditLog>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(auditLogRepository.findAll(pageable));
    }

    @GetMapping("/audit-logs/user/{email}")
    public ResponseEntity<Page<AuditLog>> getAuditLogsByUser(
            @PathVariable String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(auditLogRepository.findByUserEmail(email, pageable));
    }

    @GetMapping("/audit-logs/events/{type}")
    public ResponseEntity<Page<AuditLog>> getAuditLogsByEventType(
            @PathVariable AuditEventType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(auditLogRepository.findByEventType(type, pageable));
    }

    // ── Category Management ──────────────────────────────────────────────────

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryDto>> getCategories() {
        return ResponseEntity.ok(adminService.getAllCategories());
    }

    @PostMapping("/categories")
    public ResponseEntity<CategoryDto> createCategory(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        Long parentId = body.get("parentId") != null ? ((Number) body.get("parentId")).longValue() : null;
        return ResponseEntity.ok(adminService.createCategory(name, parentId));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<CategoryDto> updateCategory(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        Long parentId = body.get("parentId") != null ? ((Number) body.get("parentId")).longValue() : null;
        return ResponseEntity.ok(adminService.updateCategory(id, name, parentId));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        adminService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    // ── Coupon Management ────────────────────────────────────────────────────

    @GetMapping("/coupons")
    public ResponseEntity<Page<CouponDto>> listCoupons(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(adminService.listCoupons(pageable));
    }

    @PostMapping("/coupons")
    public ResponseEntity<CouponDto> createCoupon(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(adminService.createCoupon(body));
    }

    @DeleteMapping("/coupons/{id}")
    public ResponseEntity<Void> deleteCoupon(@PathVariable Long id) {
        adminService.deleteCoupon(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/coupons/{id}/toggle")
    public ResponseEntity<CouponDto> toggleCoupon(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.toggleCoupon(id));
    }

    // ── Order Management ─────────────────────────────────────────────────────

    @GetMapping("/orders")
    public ResponseEntity<Page<OrderResponse>> listOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(adminService.listAllOrders(status, pageable));
    }

    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(adminService.updateOrderStatus(id, status));
    }

    // ── Product Management ───────────────────────────────────────────────────

    @GetMapping("/products")
    public ResponseEntity<Page<ProductResponse>> listProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(adminService.listAllProducts(keyword, pageable));
    }
}
