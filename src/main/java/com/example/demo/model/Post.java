package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String content;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<PostCollaboration> collaborations = new java.util.ArrayList<>();

    private LocalDateTime createdAt;

    private String mediaUrl;
    private String mediaType; // "IMAGE" or "VIDEO"
    private String hashtags;

    public Post() {
        this.createdAt = LocalDateTime.now();
    }

    public Post(String content, User user, String mediaUrl, String mediaType, String hashtags) {
        this.content = content;
        this.user = user;
        this.mediaUrl = mediaUrl;
        this.mediaType = mediaType;
        this.hashtags = hashtags;
        this.createdAt = LocalDateTime.now();
    }

    public Post(String content, User user) {
        this.content = content;
        this.user = user;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getHashtags() {
        return hashtags;
    }

    public void setHashtags(String hashtags) {
        this.hashtags = hashtags;
    }

    public java.util.List<PostCollaboration> getCollaborations() {
        return collaborations;
    }

    public void setCollaborations(java.util.List<PostCollaboration> collaborations) {
        this.collaborations = collaborations;
    }
}
