package com.ecommerce.main.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TwoFactorSetupResponse {
    private String secret;
    private String qrCodeDataUri;
}
