package com.example.demo.repository;

import com.example.demo.model.ChatMessage;
import com.example.demo.model.MessageReadReceipt;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MessageReadReceiptRepository extends JpaRepository<MessageReadReceipt, Long> {
    Optional<MessageReadReceipt> findByMessageAndUser(ChatMessage message, User user);

    boolean existsByMessageAndUser(ChatMessage message, User user);
}
