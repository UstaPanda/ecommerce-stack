package com.ecommerce.main.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TwoFactorCodeRequest {
    private String code;
}
