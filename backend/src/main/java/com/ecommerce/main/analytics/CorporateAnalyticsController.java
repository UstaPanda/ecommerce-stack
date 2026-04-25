package com.ecommerce.main.analytics;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class CorporateAnalyticsController {

    private final CorporateAnalyticsService analyticsService;

    /**
     * Corporate/Admin: store KPI dashboard
     * from/to optional — defaults to last 30 days
     */
    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasAnyAuthority('ROLE_CORPORATE', 'ROLE_ADMIN')")
    public ResponseEntity<CorporateAnalyticsResponse> getStoreAnalytics(
            @PathVariable Long storeId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Authentication auth) {

        // If 'from' is null, it signals a 'Lifetime' request. 
        // The service will handle the default (e.g., Year 2000).
        return ResponseEntity.ok(
                analyticsService.getStoreAnalytics(storeId, auth.getName(), from, to)
        );
    }
}
