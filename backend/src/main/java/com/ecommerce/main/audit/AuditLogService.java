package com.ecommerce.main.audit;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Async
    public void log(AuditEventType eventType, String userEmail, String ipAddress, String device, String details) {
        AuditLog log = AuditLog.builder()
                .eventType(eventType)
                .userEmail(userEmail)
                .ipAddress(ipAddress)
                .device(device)
                .details(details)
                .build();
        auditLogRepository.save(log);
    }

    @Async
    public void log(AuditEventType eventType, String userEmail, String ipAddress, String device) {
        log(eventType, userEmail, ipAddress, device, null);
    }
}
