package com.example.demo.controller;

import com.example.demo.model.Post;
import com.example.demo.model.User;
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stories")
public class StoryApiController {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserStories(@PathVariable Long userId, HttpSession session) {
        Object sessionUser = session.getAttribute("user");
        if (sessionUser == null && session.getAttribute("userId") == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        List<Post> activeStories = postRepository.findByUserAndPostTypeAndCreatedAtAfterOrderByCreatedAtAsc(
                user, "STORY", LocalDateTime.now().minusHours(24));

        List<Map<String, Object>> response = new ArrayList<>();
        for (Post post : activeStories) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", post.getId());
            map.put("mediaUrl", post.getMediaUrl());
            map.put("mediaType", post.getMediaType());
            map.put("content", post.getContent());
            map.put("createdAt", post.getCreatedAt().toString());
            response.add(map);
        }

        return ResponseEntity.ok(response);
    }
}
