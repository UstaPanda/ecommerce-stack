package com.ecommerce.main.chat;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatAskRequest {

    @NotBlank(message = "Question is required")
    private String question;

    // Optional: continue an existing session
    private Long sessionId;
}
