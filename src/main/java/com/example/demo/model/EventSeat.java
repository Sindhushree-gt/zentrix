package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_seats")
public class EventSeat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String rowLabel;    // A, B, C...
    private Integer seatNumber; // 1, 2, 3...
    
    private String seatType;    // REGULAR, VIP, GOLD
    private Double price;
    
    private String status = "AVAILABLE"; // AVAILABLE, BOOKED, HOLD
    
    private LocalDateTime holdExpiresAt;

    @ManyToOne
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User bookedByUser;

    public EventSeat() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRowLabel() { return rowLabel; }
    public void setRowLabel(String rowLabel) { this.rowLabel = rowLabel; }

    public Integer getSeatNumber() { return seatNumber; }
    public void setSeatNumber(Integer seatNumber) { this.seatNumber = seatNumber; }

    public String getSeatType() { return seatType; }
    public void setSeatType(String seatType) { this.seatType = seatType; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getHoldExpiresAt() { return holdExpiresAt; }
    public void setHoldExpiresAt(LocalDateTime holdExpiresAt) { this.holdExpiresAt = holdExpiresAt; }

    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }

    public User getBookedByUser() { return bookedByUser; }
    public void setBookedByUser(User bookedByUser) { this.bookedByUser = bookedByUser; }

    public String getSeatIdentifier() {
        return rowLabel + seatNumber;
    }
}
