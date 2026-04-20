package com.example.demo.music.room;

import com.example.demo.model.User;
import com.example.demo.music.MusicService;
import com.example.demo.music.Track;
import com.example.demo.music.TrackRepository;
import com.example.demo.music.TrackStatus;
import com.example.demo.music.MusicStorageService;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.RewardService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

@Controller
public class MusicRoomController {

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MusicRoomService roomService;

    @Autowired
    private MusicRoomRepository roomRepository;

    @Autowired
    private MusicRoomSubmissionRepository submissionRepository;

    @Autowired
    private MusicRoomVoteRepository voteRepository;

    @Autowired
    private MusicService musicService;

    @Autowired
    private MusicStorageService storageService;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private RewardService rewardService;

    @Autowired
    private MusicRoomWebSocketController ws;

    private User getUser(HttpSession session) {
        Object authUser = httpServletRequest.getAttribute("authenticatedUser");
        if (authUser instanceof User) return (User) authUser;
        Object userIdObj = session.getAttribute("userId");
        if (userIdObj instanceof Long) {
            return userRepository.findById((Long) userIdObj).orElse(null);
        }
        return null;
    }

    @GetMapping("/music/rooms")
    public String roomsHome(Model model, HttpSession session) {
        User user = getUser(session);
        if (user == null) return "redirect:/login";

        model.addAttribute("user", user);
        model.addAttribute("approvedTracks", musicService.listApproved());
        return "music-rooms";
    }

    @PostMapping("/music/rooms/create")
    public String createRoom(@RequestParam("name") String name,
                             @RequestParam(value = "category", required = false) String category,
                             @RequestParam(value = "trackId", required = false) Long trackId,
                             @RequestParam(value = "localFile", required = false) MultipartFile localFile,
                             @RequestParam(value = "localTitle", required = false) String localTitle,
                             @RequestParam(value = "localArtistName", required = false) String localArtistName,
                             @RequestParam(value = "acceptLocalTerms", required = false) Boolean acceptLocalTerms,
                             HttpSession session) {
        User user = getUser(session);
        if (user == null) return "redirect:/login";

        MusicRoom room = roomService.createRoom(user, name, category);

        Long submitId = trackId;
        if (submitId == null && localFile != null && !localFile.isEmpty() && Boolean.TRUE.equals(acceptLocalTerms)) {
            submitId = createRoomOnlyTrack(user, localTitle, localArtistName, localFile);
        }

        if (submitId != null) {
            roomService.submitTrack(room, user, submitId);
            ws.broadcastRoomEvent(room.getCode(), Map.of("type", "submission"));
        }
        return "redirect:/music/rooms/" + room.getCode();
    }

    @PostMapping("/music/rooms/join")
    public String joinRoom(@RequestParam("code") String code,
                           @RequestParam(value = "trackId", required = false) Long trackId,
                           @RequestParam(value = "localFile", required = false) MultipartFile localFile,
                           @RequestParam(value = "localTitle", required = false) String localTitle,
                           @RequestParam(value = "localArtistName", required = false) String localArtistName,
                           @RequestParam(value = "acceptLocalTerms", required = false) Boolean acceptLocalTerms,
                           HttpSession session) {
        User user = getUser(session);
        if (user == null) return "redirect:/login";

        String cleaned = code == null ? "" : code.trim().toUpperCase();
        MusicRoom room = roomService.getByCode(cleaned);
        if (room == null) return "redirect:/music/rooms?error=notfound";

        Long submitId = trackId;
        if (submitId == null && localFile != null && !localFile.isEmpty() && Boolean.TRUE.equals(acceptLocalTerms)) {
            submitId = createRoomOnlyTrack(user, localTitle, localArtistName, localFile);
        }
        if (submitId != null) {
            roomService.submitTrack(room, user, submitId);
            ws.broadcastRoomEvent(room.getCode(), Map.of("type", "submission"));
        }
        return "redirect:/music/rooms/" + room.getCode();
    }

    private Long createRoomOnlyTrack(User user, String title, String artistName, MultipartFile file) {
        try {
            String ct = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
            if (!ct.startsWith("audio/")) return null;

            MusicStorageService.StoredFile stored = storageService.store(file);

            Track t = new Track();
            t.setTitle((title == null || title.isBlank()) ? "Local Upload" : title.trim());
            t.setArtistName((artistName == null || artistName.isBlank()) ? user.getUsername() : artistName.trim());
            t.setLicenseName("Room-only upload (user provided)");
            t.setLicenseUrl("");
            t.setStoragePath(stored.absolutePath());
            t.setContentType(stored.contentType());
            t.setSizeBytes(stored.sizeBytes());
            t.setStatus(TrackStatus.ROOM_ONLY);
            t.setUploader(user);
            t.setCreatedAt(LocalDateTime.now());
            t.setApprovedAt(LocalDateTime.now());
            trackRepository.save(t);
            return t.getId();
        } catch (Exception e) {
            return null;
        }
    }

    @GetMapping("/music/rooms/{code}")
    public String viewRoom(@PathVariable String code, Model model, HttpSession session) {
        User user = getUser(session);
        if (user == null) return "redirect:/login";

        MusicRoom room = roomService.getByCode(code.trim().toUpperCase());
        if (room == null) return "redirect:/music/rooms?error=notfound";

        boolean isHost = room.getHost() != null && room.getHost().getId() != null && room.getHost().getId().equals(user.getId());
        // Mark that at least one other user joined (stops waiting timer)
        if (!isHost && !room.isGuestJoined()) {
            room.setGuestJoined(true);
            room.setGuestJoinedAt(LocalDateTime.now());
            roomRepository.save(room);
            ws.broadcastRoomEvent(room.getCode(), Map.of("type", "guest-joined"));
        }

        List<MusicRoomSubmission> submissions = roomService.listSubmissions(room);
        Map<Long, Long> voteCounts = roomService.voteCounts(room);

        List<Track> approved = musicService.listApproved();

        Map<Long, Boolean> votedByMe = new HashMap<>();
        for (MusicRoomSubmission s : submissions) {
            boolean voted = voteRepository.existsByRoomAndSubmissionAndVoter(room, s, user);
            votedByMe.put(s.getId(), voted);
        }

        model.addAttribute("user", user);
        model.addAttribute("room", room);
        model.addAttribute("submissions", submissions);
        model.addAttribute("voteCounts", voteCounts);
        model.addAttribute("approvedTracks", approved);
        model.addAttribute("votedByMe", votedByMe);
        model.addAttribute("isHost", isHost);
        return "music-room";
    }

    @PostMapping("/music/rooms/{code}/submit")
    public String submit(@PathVariable String code, @RequestParam("trackId") Long trackId, HttpSession session) {
        User user = getUser(session);
        if (user == null) return "redirect:/login";

        MusicRoom room = roomService.getByCode(code.trim().toUpperCase());
        if (room == null) return "redirect:/music/rooms?error=notfound";

        if (room.isSubmissionsLocked() || "VOTE".equalsIgnoreCase(room.getPhase()) || "ENDED".equalsIgnoreCase(room.getPhase())) {
            return "redirect:/music/rooms/" + room.getCode() + "?error=submissions_locked";
        }

        boolean ok = roomService.submitTrack(room, user, trackId);
        if (ok) ws.broadcastRoomEvent(room.getCode(), Map.of("type", "submission"));
        return "redirect:/music/rooms/" + room.getCode() + (ok ? "" : "?error=submit");
    }

    @PostMapping("/music/rooms/{code}/vote/{submissionId}")
    public String vote(@PathVariable String code, @PathVariable Long submissionId, HttpSession session) {
        User user = getUser(session);
        if (user == null) return "redirect:/login";

        MusicRoom room = roomService.getByCode(code.trim().toUpperCase());
        if (room == null) return "redirect:/music/rooms?error=notfound";

        if (room.isVotingLocked() || "SUBMIT".equalsIgnoreCase(room.getPhase()) || "ENDED".equalsIgnoreCase(room.getPhase())) {
            return "redirect:/music/rooms/" + room.getCode() + "?error=voting_locked";
        }

        MusicRoomSubmission submission = submissionRepository.findById(submissionId).orElse(null);
        if (submission == null || submission.getRoom() == null || !submission.getRoom().getId().equals(room.getId())) {
            return "redirect:/music/rooms/" + room.getCode() + "?error=vote";
        }

        boolean voted = roomService.vote(room, submission, user);
        if (voted) {
            rewardService.awardMusicVote(user);
            ws.broadcastRoomEvent(room.getCode(), Map.of("type", "vote"));
        }
        return "redirect:/music/rooms/" + room.getCode() + (voted ? "" : "?info=already_voted");
    }

    // ---- Host controls ----

    @PostMapping("/music/rooms/{code}/host/lock-submissions")
    public String lockSubmissions(@PathVariable String code, @RequestParam boolean locked, HttpSession session) {
        User user = getUser(session);
        if (user == null) return "redirect:/login";

        MusicRoom room = roomService.getByCode(code.trim().toUpperCase());
        if (room == null) return "redirect:/music/rooms?error=notfound";
        if (room.getHost() == null || !room.getHost().getId().equals(user.getId())) return "redirect:/music/rooms/" + room.getCode() + "?error=not_host";

        room.setSubmissionsLocked(locked);
        roomRepository.save(room);
        ws.broadcastRoomEvent(room.getCode(), Map.of("type", "lock-submissions", "locked", locked));
        return "redirect:/music/rooms/" + room.getCode();
    }

    @PostMapping("/music/rooms/{code}/host/lock-voting")
    public String lockVoting(@PathVariable String code, @RequestParam boolean locked, HttpSession session) {
        User user = getUser(session);
        if (user == null) return "redirect:/login";

        MusicRoom room = roomService.getByCode(code.trim().toUpperCase());
        if (room == null) return "redirect:/music/rooms?error=notfound";
        if (room.getHost() == null || !room.getHost().getId().equals(user.getId())) return "redirect:/music/rooms/" + room.getCode() + "?error=not_host";

        room.setVotingLocked(locked);
        roomRepository.save(room);
        ws.broadcastRoomEvent(room.getCode(), Map.of("type", "lock-voting", "locked", locked));
        return "redirect:/music/rooms/" + room.getCode();
    }

    @PostMapping("/music/rooms/{code}/host/start-voting")
    public String startVoting(@PathVariable String code, @RequestParam(defaultValue = "300") int seconds, HttpSession session) {
        User user = getUser(session);
        if (user == null) return "redirect:/login";

        MusicRoom room = roomService.getByCode(code.trim().toUpperCase());
        if (room == null) return "redirect:/music/rooms?error=notfound";
        if (room.getHost() == null || !room.getHost().getId().equals(user.getId())) return "redirect:/music/rooms/" + room.getCode() + "?error=not_host";

        int safe = Math.max(30, Math.min(seconds, 1800));
        room.setPhase("VOTE");
        room.setSubmissionsLocked(true);
        room.setVotingLocked(false);
        room.setCountdownEndsAt(LocalDateTime.now().plusSeconds(safe));
        roomRepository.save(room);
        ws.broadcastRoomEvent(room.getCode(), Map.of("type", "phase", "phase", "VOTE", "countdownEndsAt", room.getCountdownEndsAt().toString()));
        return "redirect:/music/rooms/" + room.getCode();
    }

    @PostMapping("/music/rooms/{code}/host/end-room")
    public String endRoom(@PathVariable String code, HttpSession session) {
        User user = getUser(session);
        if (user == null) return "redirect:/login";

        MusicRoom room = roomService.getByCode(code.trim().toUpperCase());
        if (room == null) return "redirect:/music/rooms?error=notfound";
        if (room.getHost() == null || !room.getHost().getId().equals(user.getId())) return "redirect:/music/rooms/" + room.getCode() + "?error=not_host";

        room.setPhase("ENDED");
        room.setVotingLocked(true);
        room.setActive(false);
        roomRepository.save(room);
        ws.broadcastRoomEvent(room.getCode(), Map.of("type", "phase", "phase", "ENDED"));
        return "redirect:/music/rooms/" + room.getCode();
    }

    @PostMapping("/music/rooms/{code}/host/declare-winner")
    public String declareWinner(@PathVariable String code, @RequestParam Long submissionId, HttpSession session) {
        User user = getUser(session);
        if (user == null) return "redirect:/login";

        MusicRoom room = roomService.getByCode(code.trim().toUpperCase());
        if (room == null) return "redirect:/music/rooms?error=notfound";
        if (room.getHost() == null || !room.getHost().getId().equals(user.getId())) return "redirect:/music/rooms/" + room.getCode() + "?error=not_host";

        MusicRoomSubmission submission = submissionRepository.findById(submissionId).orElse(null);
        if (submission == null || submission.getRoom() == null || !submission.getRoom().getId().equals(room.getId())) {
            return "redirect:/music/rooms/" + room.getCode() + "?error=winner";
        }

        room.setWinnerSubmission(submission);
        room.setPhase("ENDED");
        room.setVotingLocked(true);
        room.setActive(false);
        roomRepository.save(room);
        ws.broadcastRoomEvent(room.getCode(), Map.of("type", "winner", "submissionId", submissionId));
        return "redirect:/music/rooms/" + room.getCode();
    }
}

