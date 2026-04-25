package com.ecommerce.main.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class DeviceResponse {
    private Long id;
    private String deviceName;
    private String ipAddress;
    private LocalDateTime lastSeen;
}
