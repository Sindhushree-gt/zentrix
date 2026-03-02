package com.example.demo.config;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class WebSocketEventListener {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes != null && sessionAttributes.containsKey("user")) {
            User user = (User) sessionAttributes.get("user");
            updateUserStatus(user.getId(), true);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes != null && sessionAttributes.containsKey("user")) {
            User user = (User) sessionAttributes.get("user");
            updateUserStatus(user.getId(), false);
        }
    }

    private void updateUserStatus(Long userId, boolean isOnline) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastActiveAt(LocalDateTime.now());
            userRepository.save(user);

            Map<String, Object> statusMessage = new HashMap<>();
            statusMessage.put("userId", userId);
            statusMessage.put("isOnline", isOnline);
            statusMessage.put("lastActiveAt", user.getLastActiveAt());

            messagingTemplate.convertAndSend((String) "/topic/online-status", (Object) statusMessage);
        });
    }
}
