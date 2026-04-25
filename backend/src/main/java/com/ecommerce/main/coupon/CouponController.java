package com.ecommerce.main.coupon;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponRepository couponRepository;

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(@RequestBody Map<String, Object> body) {
        String code = ((String) body.get("code")).toUpperCase().trim();
        double total = ((Number) body.getOrDefault("total", 0)).doubleValue();

        Coupon coupon = couponRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new IllegalArgumentException("Geçersiz kupon kodu."));

        if (!coupon.getActive())
            throw new IllegalArgumentException("Kupon artık aktif değil.");
        if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("Kuponun süresi dolmuş.");
        if (coupon.getMaxUses() != null && coupon.getUsedCount() >= coupon.getMaxUses())
            throw new IllegalArgumentException("Kupon kullanım limiti dolmuş.");

        BigDecimal totalDecimal = BigDecimal.valueOf(total);
        BigDecimal discount;
        if ("PERCENTAGE".equals(coupon.getDiscountType())) {
            discount = totalDecimal.multiply(coupon.getDiscountValue()).divide(BigDecimal.valueOf(100));
        } else {
            discount = coupon.getDiscountValue().min(totalDecimal);
        }

        return ResponseEntity.ok(Map.of(
                "valid", true,
                "discountAmount", discount,
                "discountType", coupon.getDiscountType(),
                "discountValue", coupon.getDiscountValue()
        ));
    }
}
