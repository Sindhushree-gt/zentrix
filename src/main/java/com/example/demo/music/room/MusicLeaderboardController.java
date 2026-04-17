package com.example.demo.music.room;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.*;

@Controller
public class MusicLeaderboardController {

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MusicRoomVoteRepository voteRepository;

    @Autowired
    private MusicRoomRepository roomRepository;

    private User getUser(HttpSession session) {
        Object authUser = httpServletRequest.getAttribute("authenticatedUser");
        if (authUser instanceof User) return (User) authUser;
        Object userIdObj = session.getAttribute("userId");
        if (userIdObj instanceof Long) {
            return userRepository.findById((Long) userIdObj).orElse(null);
        }
        return null;
    }

    @GetMapping("/music/leaderboard")
    public String leaderboard(Model model, HttpSession session) {
        User user = getUser(session);
        if (user == null) return "redirect:/login";

        // Simple MVP leaderboard:
        // - Top voters = users with most votes cast
        // - Top hosts = users with most rooms created
        Map<Long, Long> votesByUser = new HashMap<>();
        for (MusicRoomVote v : voteRepository.findAll()) {
            if (v.getVoter() == null || v.getVoter().getId() == null) continue;
            votesByUser.merge(v.getVoter().getId(), 1L, Long::sum);
        }

        Map<Long, Long> roomsByHost = new HashMap<>();
        for (MusicRoom r : roomRepository.findAll()) {
            if (r.getHost() == null || r.getHost().getId() == null) continue;
            roomsByHost.merge(r.getHost().getId(), 1L, Long::sum);
        }

        model.addAttribute("user", user);
        model.addAttribute("topVoters", topUsers(votesByUser, 10));
        model.addAttribute("topHosts", topUsers(roomsByHost, 10));
        return "music-leaderboard";
    }

    private List<Map<String, Object>> topUsers(Map<Long, Long> counts, int limit) {
        List<Map.Entry<Long, Long>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        if (sorted.size() > limit) sorted = sorted.subList(0, limit);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<Long, Long> e : sorted) {
            User u = userRepository.findById(e.getKey()).orElse(null);
            if (u == null) continue;
            Map<String, Object> row = new HashMap<>();
            row.put("username", u.getUsername());
            row.put("count", e.getValue());
            rows.add(row);
        }
        return rows;
    }
}

