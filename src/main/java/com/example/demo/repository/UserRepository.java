package com.example.demo.repository;

import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
    java.util.List<User> findByUsernameContainingIgnoreCase(String username);

    java.util.List<User> findAllByOrderByXpDesc();
    
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "DELETE FROM user_voted_events WHERE event_id = ?1", nativeQuery = true)
    void deleteFromVotedEvents(Long eventId);
}
