package com.example.demo.music.room;

import com.example.demo.model.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "music_rooms")
public class MusicRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 12)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 40)
    private String category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_user_id", nullable = false)
    private User host;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private boolean active = true;

    // Waiting-room timer (before anyone else joins)
    private LocalDateTime waitingEndsAt;

    @Column(nullable = false)
    private boolean guestJoined = false;

    private LocalDateTime guestJoinedAt;

    // Live room controls (host-managed)
    @Column(nullable = false)
    private boolean submissionsLocked = false;

    @Column(nullable = false)
    private boolean votingLocked = false;

    private LocalDateTime countdownEndsAt;

    @Column(length = 20, nullable = false)
    private String phase = "SUBMIT"; // SUBMIT, VOTE, ENDED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_submission_id")
    private MusicRoomSubmission winnerSubmission;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public User getHost() {
        return host;
    }

    public void setHost(User host) {
        this.host = host;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getWaitingEndsAt() {
        return waitingEndsAt;
    }

    public void setWaitingEndsAt(LocalDateTime waitingEndsAt) {
        this.waitingEndsAt = waitingEndsAt;
    }

    public boolean isGuestJoined() {
        return guestJoined;
    }

    public void setGuestJoined(boolean guestJoined) {
        this.guestJoined = guestJoined;
    }

    public LocalDateTime getGuestJoinedAt() {
        return guestJoinedAt;
    }

    public void setGuestJoinedAt(LocalDateTime guestJoinedAt) {
        this.guestJoinedAt = guestJoinedAt;
    }

    public boolean isSubmissionsLocked() {
        return submissionsLocked;
    }

    public void setSubmissionsLocked(boolean submissionsLocked) {
        this.submissionsLocked = submissionsLocked;
    }

    public boolean isVotingLocked() {
        return votingLocked;
    }

    public void setVotingLocked(boolean votingLocked) {
        this.votingLocked = votingLocked;
    }

    public LocalDateTime getCountdownEndsAt() {
        return countdownEndsAt;
    }

    public void setCountdownEndsAt(LocalDateTime countdownEndsAt) {
        this.countdownEndsAt = countdownEndsAt;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public MusicRoomSubmission getWinnerSubmission() {
        return winnerSubmission;
    }

    public void setWinnerSubmission(MusicRoomSubmission winnerSubmission) {
        this.winnerSubmission = winnerSubmission;
    }
}

