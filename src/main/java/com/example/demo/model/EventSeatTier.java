package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
@Table(name = "event_seat_tiers")
public class EventSeatTier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tierName;
    private Double price;
    private Integer capacity;
    
    @Column(nullable = false)
    private Integer registeredCount = 0;

    @ManyToOne
    @JoinColumn(name = "event_id")
    @JsonBackReference
    private Event event;

    public EventSeatTier() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTierName() { return tierName; }
    public void setTierName(String tierName) { this.tierName = tierName; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public Integer getRegisteredCount() { return registeredCount != null ? registeredCount : 0; }
    public void setRegisteredCount(Integer registeredCount) { this.registeredCount = registeredCount; }

    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
}
