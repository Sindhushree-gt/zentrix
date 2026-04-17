package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.service.ChatService;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = org.springframework.stereotype.Controller.class)
public class GlobalModelAttributeAdvice {

    @Autowired
    private ChatService chatService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private jakarta.servlet.http.HttpServletRequest httpServletRequest;

    @ModelAttribute
    public void addGlobalAttributes(HttpSession session, Model model) {
        User user = getUserFromSession(session);
        if (user != null) {
            long unreadCount = chatService.getUnreadCount(user);
            model.addAttribute("unreadMessageCount", unreadCount);
            model.addAttribute("currentUser", user); // Add this for UI
        }

        Object token = httpServletRequest.getAttribute("urlToken");
        if (token instanceof String && !((String) token).isBlank()) {
            model.addAttribute("auth", token);
        }
    }

    private User getUserFromSession(HttpSession session) {
        // First check request attribute from JWT
        Object authUser = httpServletRequest.getAttribute("authenticatedUser");
        if (authUser instanceof User) {
            return (User) authUser;
        }

        if (session == null)
            return null;
        Object sessionUser = session.getAttribute("user");
        if (sessionUser instanceof User) {
            return (User) sessionUser;
        }
        Object userIdObj = session.getAttribute("userId");
        if (userIdObj != null) {
            try {
                Long userId = null;
                if (userIdObj instanceof Number) {
                    userId = ((Number) userIdObj).longValue();
                } else if (userIdObj instanceof String) {
                    userId = Long.parseLong((String) userIdObj);
                }

                if (userId != null) {
                    return userRepository.findById(userId).orElse(null);
                }
            } catch (Exception e) {
                // Ignore recovery failure
            }
        }
        return null;
    }
}
