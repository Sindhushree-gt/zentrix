package com.example.demo.music.room;

import com.example.demo.model.User;
import com.example.demo.music.Track;
import com.example.demo.music.TrackRepository;
import com.example.demo.music.TrackStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MusicRoomService {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RAND = new SecureRandom();

    @Autowired
    private MusicRoomRepository roomRepository;

    @Autowired
    private MusicRoomSubmissionRepository submissionRepository;

    @Autowired
    private MusicRoomVoteRepository voteRepository;

    @Autowired
    private TrackRepository trackRepository;

    public MusicRoom createRoom(User host, String name, String category) {
        MusicRoom room = new MusicRoom();
        room.setHost(host);
        room.setName((name == null || name.isBlank()) ? "Music Room" : name.trim());
        room.setCategory(category == null ? null : category.trim());
        room.setCode(generateUniqueCode(6));
        room.setWaitingEndsAt(LocalDateTime.now().plusMinutes(2));
        return roomRepository.save(room);
    }

    public MusicRoom getByCode(String code) {
        return roomRepository.findByCode(code).orElse(null);
    }

    public List<MusicRoomSubmission> listSubmissions(MusicRoom room) {
        return submissionRepository.findByRoomOrderByCreatedAtAsc(room);
    }

    public Map<Long, Long> voteCounts(MusicRoom room) {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : voteRepository.countVotesBySubmission(room)) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Transactional
    public boolean submitTrack(MusicRoom room, User submitter, Long trackId) {
        Track track = trackRepository.findById(trackId).orElse(null);
        if (track == null || track.getStatus() != TrackStatus.APPROVED) return false;

        if (submissionRepository.findByRoomAndTrackId(room, trackId).isPresent()) {
            return true; // already submitted
        }

        MusicRoomSubmission sub = new MusicRoomSubmission();
        sub.setRoom(room);
        sub.setTrack(track);
        sub.setSubmittedBy(submitter);
        submissionRepository.save(sub);
        return true;
    }

    @Transactional
    public boolean vote(MusicRoom room, MusicRoomSubmission submission, User voter) {
        if (voteRepository.existsByRoomAndSubmissionAndVoter(room, submission, voter)) return false;
        MusicRoomVote v = new MusicRoomVote();
        v.setRoom(room);
        v.setSubmission(submission);
        v.setVoter(voter);
        voteRepository.save(v);
        return true;
    }

    private String generateUniqueCode(int len) {
        for (int attempt = 0; attempt < 20; attempt++) {
            String code = randomCode(len);
            if (!roomRepository.existsByCode(code)) return code;
        }
        // fallback (extremely unlikely)
        return randomCode(len) + randomCode(2);
    }

    private String randomCode(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(CODE_CHARS.charAt(RAND.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}

