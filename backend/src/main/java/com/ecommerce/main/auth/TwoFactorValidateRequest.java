package com.ecommerce.main.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TwoFactorValidateRequest {
    private String tempToken;
    private String code;
}
