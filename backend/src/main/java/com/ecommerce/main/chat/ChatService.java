package com.ecommerce.main.chat;

import com.ecommerce.main.store.StoreRepository;
import com.ecommerce.main.user.Role;
import com.ecommerce.main.user.User;
import com.ecommerce.main.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final QueryExecutionService queryExecutionService;

    @Value("${chatbot.service.url:http://localhost:8000}")
    private String chatbotServiceUrl;

    /**
     * Main entry: receives user question, forwards to Python/LangGraph,
     * executes generated SQL, persists messages, returns answer.
     */
    @Transactional
    public ChatAskResponse ask(String email, ChatAskRequest request) {
        User user = findUser(email);

        // Get or create session
        ChatSession session = resolveSession(user, request.getSessionId());

        // Persist user message
        ChatMessage userMsg = ChatMessage.builder()
                .session(session)
                .role("user")
                .content(request.getQuestion())
                .build();
        session.getMessages().add(userMsg);

        // Build role-scoped context for the AI service
        Map<String, Object> aiPayload = buildAiPayload(user, session, request.getQuestion());

        // Call Python/LangGraph service
        AiServiceResponse aiResponse = callAiService(aiPayload);

        // If AI generated SQL, execute it and attach results
        String finalAnswer = aiResponse.finalAnswer();
        if (aiResponse.sqlQuery() != null && !aiResponse.sqlQuery().isBlank()) {
            try {
                Long scopeId = resolveScopeId(user);
                List<Map<String, Object>> rows = queryExecutionService.execute(
                        aiResponse.sqlQuery(), user.getRoleType(), scopeId);
                // Re-call AI with query results for natural language explanation
                aiPayload.put("query_result", rows);
                aiPayload.put("sql_query", aiResponse.sqlQuery());
                AiServiceResponse analysisResponse = callAiService(aiPayload);
                finalAnswer = analysisResponse.finalAnswer();
            } catch (Exception e) {
                finalAnswer = "Query execution failed: " + e.getMessage();
            }
        }

        // Persist assistant message
        ChatMessage assistantMsg = ChatMessage.builder()
                .session(session)
                .role("assistant")
                .content(finalAnswer)
                .sqlQuery(aiResponse.sqlQuery())
                .visualizationCode(aiResponse.visualizationCode())
                .build();
        session.getMessages().add(assistantMsg);

        ChatSession saved = sessionRepository.save(session);
        ChatMessage savedMsg = saved.getMessages().get(saved.getMessages().size() - 1);

        return new ChatAskResponse(
                saved.getId(),
                savedMsg.getId(),
                finalAnswer,
                aiResponse.sqlQuery(),
                aiResponse.visualizationCode()
        );
    }

    @Transactional(readOnly = true)
    public Page<ChatSession> getMySessions(String email, Pageable pageable) {
        return sessionRepository.findByUserEmailOrderByUpdatedAtDesc(email, pageable);
    }

    @Transactional(readOnly = true)
    public ChatSession getSession(Long sessionId, String email) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        if (!session.getUser().getEmail().equals(email)) {
            throw new IllegalStateException("Access denied");
        }
        return session;
    }

    @Transactional
    public void deleteSession(Long sessionId, String email) {
        ChatSession session = getSession(sessionId, email);
        sessionRepository.delete(session);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private ChatSession resolveSession(User user, Long sessionId) {
        if (sessionId != null) {
            ChatSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
            if (!session.getUser().getId().equals(user.getId())) {
                throw new IllegalStateException("Access denied");
            }
            return session;
        }
        ChatSession newSession = ChatSession.builder().user(user).build();
        return sessionRepository.save(newSession);
    }

    private Map<String, Object> buildAiPayload(User user, ChatSession session, String question) {
        // Last 10 messages for context
        List<ChatMessage> allMessages = session.getMessages();
        int fromIndex = Math.max(0, allMessages.size() - 10);
        List<Map<String, String>> history = allMessages.subList(fromIndex, allMessages.size())
                .stream()
                .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
                .toList();

        return new java.util.HashMap<>(Map.of(
                "question", question,
                "role", user.getRoleType().name(),
                "user_id", user.getId(),
                "history", history
        ));
    }

    private AiServiceResponse callAiService(Map<String, Object> payload) {
        try {
            RestClient client = RestClient.create();
            return client.post()
                    .uri(chatbotServiceUrl + "/ask")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(AiServiceResponse.class);
        } catch (Exception e) {
            // AI service unavailable — return graceful fallback
            return new AiServiceResponse(
                    "AI service is currently unavailable. Please try again later.",
                    null, null);
        }
    }

    private Long resolveScopeId(User user) {
        if (user.getRoleType() == Role.INDIVIDUAL) return user.getId();
        if (user.getRoleType() == Role.CORPORATE) {
            return storeRepository.findByOwnerEmail(user.getEmail()).stream()
                    .findFirst()
                    .map(s -> s.getId())
                    .orElse(null);
        }
        return null; // ADMIN: no scope restriction
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
    }

    // Internal record for AI service response deserialization
    record AiServiceResponse(
            String finalAnswer,
            String sqlQuery,
            String visualizationCode
    ) {}
}
