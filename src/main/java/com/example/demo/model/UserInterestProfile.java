package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_interest_profiles")
public class UserInterestProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private double fitnessScore = 0.0;
    private double foodScore = 0.0;
    private double travelScore = 0.0;
    private double gamingScore = 0.0;
    private double fashionScore = 0.0;
    private double lifestyleScore = 0.0;

    public UserInterestProfile() {
    }

    public UserInterestProfile(User user) {
        this.user = user;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public double getFitnessScore() {
        return fitnessScore;
    }

    public void setFitnessScore(double fitnessScore) {
        this.fitnessScore = fitnessScore;
    }

    public double getFoodScore() {
        return foodScore;
    }

    public void setFoodScore(double foodScore) {
        this.foodScore = foodScore;
    }

    public double getTravelScore() {
        return travelScore;
    }

    public void setTravelScore(double travelScore) {
        this.travelScore = travelScore;
    }

    public double getGamingScore() {
        return gamingScore;
    }

    public void setGamingScore(double gamingScore) {
        this.gamingScore = gamingScore;
    }

    public double getFashionScore() {
        return fashionScore;
    }

    public void setFashionScore(double fashionScore) {
        this.fashionScore = fashionScore;
    }

    public double getLifestyleScore() {
        return lifestyleScore;
    }

    public void setLifestyleScore(double lifestyleScore) {
        this.lifestyleScore = lifestyleScore;
    }

    public void incrementScore(ReelCategory category, double amount) {
        switch (category) {
            case FITNESS -> fitnessScore += amount;
            case FOOD -> foodScore += amount;
            case TRAVEL -> travelScore += amount;
            case GAMING -> gamingScore += amount;
            case FASHION -> fashionScore += amount;
            case LIFESTYLE -> lifestyleScore += amount;
        }
    }
}
