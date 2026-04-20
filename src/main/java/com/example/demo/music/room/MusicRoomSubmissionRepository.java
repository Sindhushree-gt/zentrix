package com.example.demo.music.room;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MusicRoomSubmissionRepository extends JpaRepository<MusicRoomSubmission, Long> {
    List<MusicRoomSubmission> findByRoomOrderByCreatedAtAsc(MusicRoom room);
    Optional<MusicRoomSubmission> findByRoomAndTrackId(MusicRoom room, Long trackId);
}

