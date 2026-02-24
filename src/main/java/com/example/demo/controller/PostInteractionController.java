package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/posts")
public class PostInteractionController {

    @Autowired
    private PostRepository postRepository;
    @Autowired
    private PostLikeRepository postLikeRepository;
    @Autowired
    private PostCommentRepository postCommentRepository;
    @Autowired
    private UserRepository userRepository;

    // ── Like / Unlike (toggle) ──────────────────────────────────────────────
    @PostMapping("/{postId}/like")
    public ResponseEntity<Map<String, Object>> toggleLike(
            @PathVariable Long postId, HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null)
            return ResponseEntity.status(401).build();
        user = userRepository.findById(user.getId()).orElse(user);

        Post post = postRepository.findById(postId).orElse(null);
        if (post == null)
            return ResponseEntity.notFound().build();

        Optional<PostLike> existing = postLikeRepository.findByPostAndUser(post, user);
        boolean liked;
        if (existing.isPresent()) {
            postLikeRepository.delete(existing.get());
            liked = false;
        } else {
            postLikeRepository.save(new PostLike(post, user));
            liked = true;
        }

        long count = postLikeRepository.countByPost(post);
        Map<String, Object> resp = new HashMap<>();
        resp.put("liked", liked);
        resp.put("count", count);
        return ResponseEntity.ok(resp);
    }

    // ── Add Comment ─────────────────────────────────────────────────────────
    @PostMapping("/{postId}/comment")
    public ResponseEntity<Map<String, Object>> addComment(
            @PathVariable Long postId,
            @RequestBody Map<String, String> body,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null)
            return ResponseEntity.status(401).build();
        user = userRepository.findById(user.getId()).orElse(user);

        Post post = postRepository.findById(postId).orElse(null);
        if (post == null)
            return ResponseEntity.notFound().build();

        String content = body.getOrDefault("content", "").trim();
        if (content.isEmpty())
            return ResponseEntity.badRequest().build();

        PostComment comment = postCommentRepository.save(new PostComment(post, user, content));

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", comment.getId());
        resp.put("username", user.getUsername());
        resp.put("photo", user.getProfilePhotoUrl() != null ? user.getProfilePhotoUrl() : "");
        resp.put("content", comment.getContent());
        resp.put("date", comment.getCreatedAt().toString());
        resp.put("count", postCommentRepository.countByPost(post));
        return ResponseEntity.ok(resp);
    }

    // ── Get Comments ────────────────────────────────────────────────────────
    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<Map<String, Object>>> getComments(
            @PathVariable Long postId, HttpSession session) {

        if (session.getAttribute("user") == null)
            return ResponseEntity.status(401).build();
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null)
            return ResponseEntity.notFound().build();

        List<Map<String, Object>> result = new ArrayList<>();
        for (PostComment c : postCommentRepository.findByPostOrderByCreatedAtAsc(post)) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("username", c.getUser().getUsername());
            m.put("photo", c.getUser().getProfilePhotoUrl() != null ? c.getUser().getProfilePhotoUrl() : "");
            m.put("content", c.getContent());
            m.put("date", c.getCreatedAt().toString());
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }

    // ── Get post stats (likes + comments count + did current user like?) ────
    @GetMapping("/{postId}/stats")
    public ResponseEntity<Map<String, Object>> getStats(
            @PathVariable Long postId, HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null)
            return ResponseEntity.status(401).build();
        user = userRepository.findById(user.getId()).orElse(user);

        Post post = postRepository.findById(postId).orElse(null);
        if (post == null)
            return ResponseEntity.notFound().build();

        Map<String, Object> resp = new HashMap<>();
        resp.put("likes", postLikeRepository.countByPost(post));
        resp.put("comments", postCommentRepository.countByPost(post));
        resp.put("liked", postLikeRepository.existsByPostAndUser(post, user));
        return ResponseEntity.ok(resp);
    }
}
