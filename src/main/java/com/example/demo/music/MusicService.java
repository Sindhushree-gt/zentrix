package com.example.demo.music;

import com.example.demo.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MusicService {

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private TrackLikeRepository trackLikeRepository;

    @Autowired
    private TrackPlayEventRepository trackPlayEventRepository;

    public List<Track> listApproved() {
        return trackRepository.findByStatusOrderByCreatedAtDesc(TrackStatus.APPROVED);
    }

    public List<Track> listPending() {
        return trackRepository.findByStatusOrderByCreatedAtDesc(TrackStatus.PENDING);
    }

    public Optional<Track> findById(Long id) {
        return trackRepository.findById(id);
    }

    public long likeCount(Track track) {
        return trackLikeRepository.countByTrack(track);
    }

    @Transactional
    public boolean toggleLike(Track track, User user) {
        boolean exists = trackLikeRepository.existsByTrackAndUser(track, user);
        if (exists) {
            trackLikeRepository.deleteByTrackAndUser(track, user);
            return false;
        }
        TrackLike like = new TrackLike();
        like.setTrack(track);
        like.setUser(user);
        trackLikeRepository.save(like);
        return true;
    }

    @Transactional
    public void recordListening(User user, Track track, int secondsListened) {
        int safeSeconds = Math.max(0, Math.min(secondsListened, 300)); // cap each ping (anti-spam)
        if (safeSeconds <= 0) return;

        TrackPlayEvent ev = new TrackPlayEvent();
        ev.setUser(user);
        ev.setTrack(track);
        ev.setSecondsListened(safeSeconds);
        trackPlayEventRepository.save(ev);
    }

    public long secondsListenedToday(User user) {
        LocalDateTime since = LocalDate.now().atStartOfDay();
        return trackPlayEventRepository.sumSecondsListenedSince(user, since);
    }
}

