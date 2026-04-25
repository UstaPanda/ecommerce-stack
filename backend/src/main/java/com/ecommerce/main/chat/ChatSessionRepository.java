package com.ecommerce.main.chat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    Page<ChatSession> findByUserEmailOrderByUpdatedAtDesc(String email, Pageable pageable);
}
