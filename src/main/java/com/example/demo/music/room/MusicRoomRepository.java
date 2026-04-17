package com.example.demo.music.room;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MusicRoomRepository extends JpaRepository<MusicRoom, Long> {
    Optional<MusicRoom> findByCode(String code);
    boolean existsByCode(String code);

    List<MusicRoom> findTop5ByActiveTrueAndPhaseNotOrderByCreatedAtDesc(String phase);
}

