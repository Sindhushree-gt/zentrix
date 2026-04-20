package com.example.demo.music;

import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface TrackPlayEventRepository extends JpaRepository<TrackPlayEvent, Long> {

    @Query("select coalesce(sum(e.secondsListened),0) from TrackPlayEvent e where e.user = :user and e.createdAt >= :since")
    long sumSecondsListenedSince(@Param("user") User user, @Param("since") LocalDateTime since);
}

