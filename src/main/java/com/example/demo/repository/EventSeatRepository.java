package com.example.demo.repository;

import com.example.demo.model.EventSeat;
import com.example.demo.model.Event;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventSeatRepository extends JpaRepository<EventSeat, Long> {
    List<EventSeat> findByEvent(Event event);
    List<EventSeat> findByStatusAndHoldExpiresAtBefore(String status, LocalDateTime now);
    List<EventSeat> findByEventAndBookedByUser(Event event, User user);
}
