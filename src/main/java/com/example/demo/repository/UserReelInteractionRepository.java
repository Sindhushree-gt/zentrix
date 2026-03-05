package com.example.demo.repository;

import com.example.demo.model.UserReelInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserReelInteractionRepository extends JpaRepository<UserReelInteraction, Long> {

    Optional<UserReelInteraction> findByUserIdAndReelId(Long userId, Long reelId);

    // Add additional queries if needed for analytics
}
