package com.ecommerce.main.chat;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /** Tüm roller: soru sor */
    @PostMapping("/ask")
    public ResponseEntity<ChatAskResponse> ask(
            @Valid @RequestBody ChatAskRequest request,
            Authentication auth) {
        return ResponseEntity.ok(chatService.ask(auth.getName(), request));
    }

    /** Tüm roller: kendi oturumlarını listele */
    @GetMapping("/sessions")
    public ResponseEntity<Page<ChatSession>> getMySessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(chatService.getMySessions(auth.getName(), pageable));
    }

    /** Tüm roller: oturum geçmişini getir */
    @GetMapping("/sessions/{id}")
    public ResponseEntity<ChatSession> getSession(
            @PathVariable Long id,
            Authentication auth) {
        return ResponseEntity.ok(chatService.getSession(id, auth.getName()));
    }

    /** Tüm roller: oturumu sil */
    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> deleteSession(
            @PathVariable Long id,
            Authentication auth) {
        chatService.deleteSession(id, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
