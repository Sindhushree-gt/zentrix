package com.example.demo.music.room;

import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MusicRoomVoteRepository extends JpaRepository<MusicRoomVote, Long> {
    boolean existsByRoomAndSubmissionAndVoter(MusicRoom room, MusicRoomSubmission submission, User voter);

    @Query("select v.submission.id, count(v) from MusicRoomVote v where v.room = :room group by v.submission.id")
    List<Object[]> countVotesBySubmission(@Param("room") MusicRoom room);
}

