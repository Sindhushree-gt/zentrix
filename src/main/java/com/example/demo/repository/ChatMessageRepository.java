package com.example.demo.repository;

import com.example.demo.model.ChatMessage;
import com.example.demo.model.Conversation;
import com.example.demo.model.MessageStatus;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

        List<ChatMessage> findByConversationOrderByTimestampAsc(Conversation conversation);

        List<ChatMessage> findByConversationAndMediaUrlIsNotNullOrderByTimestampDesc(Conversation conversation);

        @org.springframework.data.jpa.repository.Modifying
        @org.springframework.transaction.annotation.Transactional
        @Query("DELETE FROM ChatMessage m WHERE m.conversation = :conv AND m.isVanish = true AND m.status = :status")
        void deleteByConversationAndIsVanishTrueAndStatus(@Param("conv") Conversation conv, @Param("status") MessageStatus status);

        @org.springframework.data.jpa.repository.Modifying
        @org.springframework.transaction.annotation.Transactional
        @Query("DELETE FROM ChatMessage m WHERE m.conversation = :conv AND m.isVanish = true")
        void deleteByConversationAndIsVanishTrue(@Param("conv") Conversation conv);

        @org.springframework.data.jpa.repository.Modifying
        @org.springframework.transaction.annotation.Transactional
        @Query(value = "DELETE FROM message_reactions WHERE message_id IN (SELECT id FROM chat_message WHERE conversation_id = :convId AND is_vanish = true)", nativeQuery = true)
        void deleteReactionsByConversationAndIsVanishTrue(@Param("convId") Long convId);

        @org.springframework.data.jpa.repository.Modifying
        @org.springframework.transaction.annotation.Transactional
        @Query(value = "DELETE FROM message_reactions WHERE message_id IN (SELECT id FROM chat_message WHERE conversation_id = :convId AND is_vanish = true AND status = :status)", nativeQuery = true)
        void deleteReactionsByConversationAndIsVanishTrueAndStatus(@Param("convId") Long convId, @Param("status") String status);

        List<ChatMessage> findByConversationAndIsPinnedTrueOrderByTimestampDesc(Conversation conversation);

        // Unread count logic for groups/many-to-many:
        // This is more complex now. A simple exists check would be:
        // Is there any message in this conversation not sent by me that I haven't read?
        @Query("SELECT COUNT(m) > 0 FROM ChatMessage m WHERE m.conversation = :conv AND m.sender <> :user " +
                        "AND NOT EXISTS (SELECT r FROM MessageReadReceipt r WHERE r.message = m AND r.user = :user)")
        boolean existsUnreadForUser(@Param("conv") Conversation conv, @Param("user") User user);

        @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.conversation IN :convs AND m.sender <> :user " +
                        "AND NOT EXISTS (SELECT r FROM MessageReadReceipt r WHERE r.message = m AND r.user = :user)")
        long countUnreadForUserInConversations(@Param("convs") List<Conversation> convs, @Param("user") User user);
}
