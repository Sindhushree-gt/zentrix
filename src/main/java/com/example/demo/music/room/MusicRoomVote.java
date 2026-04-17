package com.example.demo.music.room;

import com.example.demo.model.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "music_room_votes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_room_voter_submission",
                columnNames = {"room_id", "voter_user_id", "submission_id"}
        )
)
public class MusicRoomVote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private MusicRoom room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private MusicRoomSubmission submission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voter_user_id", nullable = false)
    private User voter;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MusicRoom getRoom() {
        return room;
    }

    public void setRoom(MusicRoom room) {
        this.room = room;
    }

    public MusicRoomSubmission getSubmission() {
        return submission;
    }

    public void setSubmission(MusicRoomSubmission submission) {
        this.submission = submission;
    }

    public User getVoter() {
        return voter;
    }

    public void setVoter(User voter) {
        this.voter = voter;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

