package com.ecommerce.main.chat;

public record ChatAskResponse(
        Long sessionId,
        Long messageId,
        String answer,
        String sqlQuery,
        String visualizationCode,
        Object visualizationData,
        boolean isOutOfScope
) {}
