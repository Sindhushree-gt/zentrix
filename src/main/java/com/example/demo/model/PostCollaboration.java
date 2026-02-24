package com.example.demo.model;

import jakarta.persistence.*;

@Entity
public class PostCollaboration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // The user who is invited to collaborate

    @Enumerated(EnumType.STRING)
    private CollaborationStatus status;

    public PostCollaboration() {
    }

    public PostCollaboration(Post post, User user, CollaborationStatus status) {
        this.post = post;
        this.user = user;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Post getPost() {
        return post;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public CollaborationStatus getStatus() {
        return status;
    }

    public void setStatus(CollaborationStatus status) {
        this.status = status;
    }
}
