package com.example.demo.repository;

import com.example.demo.model.EventSeatTier;
import com.example.demo.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EventSeatTierRepository extends JpaRepository<EventSeatTier, Long> {
    List<EventSeatTier> findByEvent(Event event);
}
