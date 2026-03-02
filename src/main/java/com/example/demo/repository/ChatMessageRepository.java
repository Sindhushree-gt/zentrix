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

        void deleteByConversationAndIsVanishTrueAndStatus(Conversation conv, MessageStatus status);

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
