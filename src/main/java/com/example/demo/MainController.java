package com.example.demo;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class MainController {

    private User getUserFromSession(HttpSession session) {
        Object sessionUser = session.getAttribute("user");
        if (sessionUser instanceof User) {
            return (User) sessionUser;
        }
        Object userIdObj = session.getAttribute("userId");
        if (userIdObj != null) {
            try {
                Long userId = null;
                if (userIdObj instanceof Number) {
                    userId = ((Number) userIdObj).longValue();
                } else if (userIdObj instanceof String) {
                    userId = Long.parseLong((String) userIdObj);
                }
                if (userId != null) {
                    return userRepository.findById(userId).orElse(null);
                }
            } catch (Exception e) {
            }
        }
        return null;
    }

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostCollaborationRepository postCollaborationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private com.example.demo.repository.EventRegistrationRepository eventRegistrationRepository;

    @GetMapping("/")
    public String root() {
        return "home";
    }

    @GetMapping("/home")
    public String home() {
        return "home";
    }

    @GetMapping("/features")
    public String features() {
        return "features";
    }

    @GetMapping("/support")
    public String support() {
        return "support";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/games")
    public String games() {
        return "games";
    }

    @GetMapping("/games/rock-paper-scissors")
    public String rockPaperScissors() {
        return "rock-paper-scissors";
    }

    @GetMapping("/games/snake-and-ladder")
    public String snakeAndLadder() {
        return "snake-and-ladder";
    }

    @GetMapping("/play-chess")
    public String playChess() {
        return "chess";
    }

    @GetMapping("/play-uno")
    public String playUno() {
        return "uno";
    }

    @GetMapping("/play-ludo")
    public String playLudo() {
        return "ludo";
    }

    @GetMapping("/play-mario")
    public String playMario() {
        return "mario";
    }

    @GetMapping("/play-bubble-shooter")
    public String playBubbleShooter() {
        return "bubble-shooter";
    }

    @GetMapping("/play-candy-crush")
    public String playCandyCrush() {
        return "candy-crush";
    }

    @GetMapping("/play-runner")
    public String playRunner() {
        return "runner";
    }

    @GetMapping("/play-car-game")
    public String playCarGame() {
        return "car-game";

    }

    @Transactional
    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        Object sessionUser = session.getAttribute("user");
        // Admin should land on admin dashboard, not student dashboard
        if ("admin".equals(sessionUser)) return "redirect:/admin";

        User user = getUserFromSession(session);
        if (user == null) {
            return "redirect:/login";
        }
        // Always refresh and ensure it's in session
        user = userRepository.findById(user.getId()).orElse(user);
        session.setAttribute("user", user);
        session.setAttribute("userId", user.getId());

        List<PostCollaboration> pendingRequests = postCollaborationRepository
                .findByUserAndStatus(user, CollaborationStatus.PENDING);
        model.addAttribute("pendingCount", pendingRequests.size());

        model.addAttribute("posts", postRepository.findAllByOrderByCreatedAtDesc());

        List<User> allUsers = userRepository.findAll();
        final User finalUser = user;
        Set<User> following = user.getFollowing();
        List<User> suggestions = allUsers.stream()
                .filter(u -> !u.getId().equals(finalUser.getId()))
                .filter(u -> !following.contains(u))
                .limit(5)
                .collect(Collectors.toList());
        model.addAttribute("suggestions", suggestions);

        model.addAttribute("notifications", notificationRepository.findByUserOrderByCreatedAtDesc(user));
        model.addAttribute("unreadNotifCount", notificationRepository.countByUserAndIsRead(user, false));

        return "dashboard";
    }

    @GetMapping("/admin")
    public String admin(Model model, HttpSession session) {
        // Only let admin session through
        if (!"admin".equals(session.getAttribute("user"))) return "redirect:/login";
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("events", eventRepository.findAll());
        model.addAttribute("totalEvents", eventRepository.count());
        model.addAttribute("upcomingEvents", eventRepository.countByStatus("UPCOMING"));
        model.addAttribute("ongoingEvents", eventRepository.countByStatus("ONGOING"));
        return "admin-dashboard";
    }

    // ── Stub routes: sidebar links that don't have full pages yet ──

    @GetMapping("/explore")
    public String explore(HttpSession session) {
        if (!isLoggedIn(session)) return "redirect:/login";
        // Explore currently shows the events page as a browse experience
        return "redirect:/events";
    }

    @GetMapping("/achievements")
    public String achievements(Model model, HttpSession session) {
        if (!isLoggedIn(session)) return "redirect:/login";
        
        Object sessionUser = session.getAttribute("user");
        if (sessionUser instanceof User) {
            User u = (User) sessionUser;
            User dbUser = userRepository.findById(u.getId()).orElse(u);
            model.addAttribute("user", dbUser);
            model.addAttribute("isAdmin", false);
            model.addAttribute("attendedCount", eventRegistrationRepository.countByUserAndAttendanceMarked(dbUser, true));
            // Fetch all COMPLETED event registrations with a position
            List<EventRegistration> userAchievements = eventRegistrationRepository.findByUser(dbUser).stream()
                    .filter(r -> r.getPosition() != null && !"Participant".equals(r.getPosition()) && !"Absent".equals(r.getPosition()))
                    .collect(Collectors.toList());
            model.addAttribute("achievements", userAchievements);
        } else {
            model.addAttribute("user", null);
            model.addAttribute("isAdmin", true);
            model.addAttribute("achievements", List.of());
        }

        List<User> leaderboard = userRepository.findAllByOrderByXpDesc();
        model.addAttribute("leaderboard", leaderboard);
        
        return "achievements";
    }



    @GetMapping("/notifications")
    public String notifications(HttpSession session) {
        if (!isLoggedIn(session)) return "redirect:/login";
        return "redirect:/dashboard";
    }

    /** True if any valid session (student or admin) exists */
    private boolean isLoggedIn(HttpSession session) {
        Object u = session.getAttribute("user");
        return u instanceof User || "admin".equals(u);
    }
}
