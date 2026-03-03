package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String imageUrl;
    private LocalDateTime dateTime;
    private String venue;
    private String price;
    private String category;
    private String organizer;

    // New fields for admin create event flow
    private String status = "UPCOMING";  // UPCOMING, ONGOING, COMPLETED
    private Integer maxParticipants;
    private Integer fixedParticipants = 0; // Pre-filled or manual count done by admin
    private String entryFeeType = "Free"; // Free or Paid
    private String eventMode = "Offline"; // Offline or Online
    private String meetingLink;           // For online events

    public Event() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getOrganizer() {
        return organizer;
    }

    public void setOrganizer(String organizer) {
        this.organizer = organizer;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(Integer maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public String getEntryFeeType() { return entryFeeType; }
    public void setEntryFeeType(String entryFeeType) { this.entryFeeType = entryFeeType; }

    public String getEventMode() { return eventMode; }
    public void setEventMode(String eventMode) { this.eventMode = eventMode; }

    public String getMeetingLink() { return meetingLink; }
    public void setMeetingLink(String meetingLink) { this.meetingLink = meetingLink; }

    public Integer getFixedParticipants() { return fixedParticipants; }
    public void setFixedParticipants(Integer fixedParticipants) { this.fixedParticipants = fixedParticipants; }
}
