package com.ecommerce.main.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TwoFactorService twoFactorService;
    private final com.ecommerce.main.security.ReCaptchaService reCaptchaService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        reCaptchaService.verify(request.getRecaptchaToken());
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        reCaptchaService.verify(request.getRecaptchaToken());
        String deviceName = httpRequest.getHeader("User-Agent");
        String ipAddress = httpRequest.getRemoteAddr();
        return ResponseEntity.ok(authService.login(request, deviceName, ipAddress));
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verify(@Valid @RequestBody VerifyRequest request) {
        return ResponseEntity.ok(authService.verifyEmail(request.getEmail(), request.getCode()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.getToken()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request.getEmail()));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword()));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<String> resendVerification(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.resendVerification(request.getEmail()));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.logout(request.getToken()));
    }

    // 2FA
    @PostMapping("/2fa/setup")
    public ResponseEntity<TwoFactorSetupResponse> setup2fa(Authentication authentication) {
        return ResponseEntity.ok(twoFactorService.setup(authentication.getName()));
    }

    @PostMapping("/2fa/confirm")
    public ResponseEntity<String> confirm2fa(Authentication authentication,
                                             @RequestBody TwoFactorCodeRequest request) {
        return ResponseEntity.ok(twoFactorService.confirm(authentication.getName(), request.getCode()));
    }

    @PostMapping("/2fa/disable")
    public ResponseEntity<String> disable2fa(Authentication authentication,
                                             @RequestBody TwoFactorCodeRequest request) {
        return ResponseEntity.ok(twoFactorService.disable(authentication.getName(), request.getCode()));
    }

    @PostMapping("/2fa/validate")
    public ResponseEntity<AuthResponse> validate2fa(@RequestBody TwoFactorValidateRequest request,
                                                    HttpServletRequest httpRequest) {
        String deviceName = httpRequest.getHeader("User-Agent");
        String ipAddress = httpRequest.getRemoteAddr();
        return ResponseEntity.ok(twoFactorService.validate(request.getTempToken(), request.getCode(), deviceName, ipAddress));
    }

    // Device tracking
    @GetMapping("/devices")
    public ResponseEntity<List<DeviceResponse>> getDevices(Authentication authentication) {
        return ResponseEntity.ok(authService.getDevices(authentication.getName()));
    }

    @DeleteMapping("/devices/{id}")
    public ResponseEntity<String> revokeDevice(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(authService.revokeDevice(authentication.getName(), id));
    }
}
