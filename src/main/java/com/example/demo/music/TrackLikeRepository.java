package com.example.demo.music;

import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackLikeRepository extends JpaRepository<TrackLike, Long> {
    boolean existsByTrackAndUser(Track track, User user);
    long countByTrack(Track track);
    void deleteByTrackAndUser(Track track, User user);
}

