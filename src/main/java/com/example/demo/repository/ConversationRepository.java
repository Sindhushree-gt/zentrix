package com.example.demo.repository;

import com.example.demo.model.Conversation;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("SELECT c FROM Conversation c WHERE c.type = 'DIRECT' AND :u1 MEMBER OF c.participants AND :u2 MEMBER OF c.participants AND SIZE(c.participants) = 2")
    Optional<Conversation> findDirectConversationBetweenUsers(@Param("u1") User u1, @Param("u2") User u2);

    @Query("SELECT c FROM Conversation c JOIN c.participants p WHERE p = :user ORDER BY c.lastMessageTime DESC")
    List<Conversation> findAllByUserOrderByLastMessageTimeDesc(@Param("user") User user);
}
