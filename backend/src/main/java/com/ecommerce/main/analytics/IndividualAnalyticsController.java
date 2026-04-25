package com.ecommerce.main.analytics;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class IndividualAnalyticsController {

    private final IndividualAnalyticsService analyticsService;

    /** Individual: kişisel harcama & sipariş istatistikleri */
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('ROLE_INDIVIDUAL')")
    public ResponseEntity<IndividualAnalyticsResponse> getMyAnalytics(Authentication auth) {
        return ResponseEntity.ok(analyticsService.getMyAnalytics(auth.getName()));
    }
}
