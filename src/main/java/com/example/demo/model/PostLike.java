package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "post_likes", uniqueConstraints = @UniqueConstraint(columnNames = { "post_id", "user_id" }))
public class PostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public PostLike() {
    }

    public PostLike(Post post, User user) {
        this.post = post;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public Post getPost() {
        return post;
    }

    public void setPost(Post p) {
        this.post = p;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User u) {
        this.user = u;
    }
}
