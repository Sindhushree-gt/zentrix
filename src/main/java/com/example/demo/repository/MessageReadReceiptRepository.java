package com.example.demo.repository;

import com.example.demo.model.ChatMessage;
import com.example.demo.model.MessageReadReceipt;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MessageReadReceiptRepository extends JpaRepository<MessageReadReceipt, Long> {
    Optional<MessageReadReceipt> findByMessageAndUser(ChatMessage message, User user);

    boolean existsByMessageAndUser(ChatMessage message, User user);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("DELETE FROM MessageReadReceipt r WHERE r.message.conversation = :conv AND r.message.isVanish = true")
    void deleteByConversationAndIsVanishTrue(@org.springframework.data.repository.query.Param("conv") com.example.demo.model.Conversation conv);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("DELETE FROM MessageReadReceipt r WHERE r.message.conversation = :conv AND r.message.isVanish = true AND r.message.status = :status")
    void deleteByConversationAndIsVanishTrueAndStatus(@org.springframework.data.repository.query.Param("conv") com.example.demo.model.Conversation conv, @org.springframework.data.repository.query.Param("status") com.example.demo.model.MessageStatus status);
}
