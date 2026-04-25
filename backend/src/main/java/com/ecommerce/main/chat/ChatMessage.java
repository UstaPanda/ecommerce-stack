package com.ecommerce.main.chat;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    // "user" or "assistant"
    @Column(nullable = false)
    private String role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // Generated SQL (if any)
    @Column(name = "sql_query", columnDefinition = "TEXT")
    private String sqlQuery;

    // Plotly JSON visualization data
    @Column(name = "visualization_data", columnDefinition = "TEXT")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String visualizationData;

    @com.fasterxml.jackson.annotation.JsonProperty("visualizationData")
    public Object getParsedVisualizationData() {
        if (visualizationData == null || visualizationData.isBlank()) return null;
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(visualizationData, Object.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Builder.Default
    @Column(name = "is_out_of_scope", nullable = false)
    private boolean isOutOfScope = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
