package com.example.demo;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.music.room.MusicRoom;
import com.example.demo.music.room.MusicRoomRepository;
import com.example.demo.service.FeedAlgorithmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class MainController {

    @Autowired
    private jakarta.servlet.http.HttpServletRequest httpServletRequest;

    private User getUserFromSession(HttpSession session) {
        // First check the request attribute (set by Interceptor)
        Object authUser = httpServletRequest.getAttribute("authenticatedUser");
        if (authUser instanceof User) {
            return (User) authUser;
        }

        // Fallback to session (existing logic)
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
    private FeedAlgorithmService feedAlgorithmService;

    @Autowired
    private PostCollaborationRepository postCollaborationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.example.demo.service.RewardService rewardService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private com.example.demo.repository.EventRegistrationRepository eventRegistrationRepository;

    @Autowired
    private FollowRequestRepository followRequestRepository;

    @Autowired
    private MusicRoomRepository musicRoomRepository;

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
    public String dashboard(
            Model model,
            HttpSession session,
            @RequestParam(required = false) String category
    ) {
        Object sessionUser = session.getAttribute("user");
        // Admin should land on admin dashboard, not student dashboard
        if ("admin".equals(sessionUser))
            return "redirect:/admin";

        User user = getUserFromSession(session);
        if (user == null) {
            return "redirect:/login";
        }
        // Always refresh and ensure it's in session
        user = userRepository.findById(user.getId()).orElse(user);
        session.setAttribute("user", user);
        session.setAttribute("userId", user.getId());

        populateDashboardCommonModel(model, user);

        List<PostCollaboration> pendingRequests = postCollaborationRepository
                .findByUserAndStatus(user, CollaborationStatus.PENDING);
        model.addAttribute("user", user);
        model.addAttribute("pendingCount", pendingRequests.size());

        String normalizedCategory = (category == null) ? null : category.trim();
        if (normalizedCategory != null && normalizedCategory.isEmpty()) normalizedCategory = null;
        if (normalizedCategory != null && "ALL".equalsIgnoreCase(normalizedCategory)) normalizedCategory = null;

        // Personalized feed (based on VIEW/LIKE/SAVE/SHARE) with optional category filter
        List<Post> personalized = feedAlgorithmService.getPersonalizedFeed(user.getId(), 0, 200);
        if (normalizedCategory != null) {
            final String catKey = normalizedCategory.trim().toUpperCase();
            personalized = personalized.stream()
                    .filter(p -> p.getCategory() != null && p.getCategory().trim().equalsIgnoreCase(catKey))
                    .toList();
        }
        model.addAttribute("posts", personalized);
        model.addAttribute("activeCategory", normalizedCategory);
        model.addAttribute("isReelsPage", false);

        List<User> allUsers = userRepository.findAll();
        final User finalUser = user;
        Set<User> following = user.getFollowing();
        List<User> suggestions = allUsers.stream()
                .filter(u -> !u.getId().equals(finalUser.getId()))
                .filter(u -> !following.contains(u))
                .limit(5)
                .collect(Collectors.toList());
        model.addAttribute("suggestions", suggestions);

        return "dashboard";
    }

    @Transactional
    @GetMapping("/reels")
    public String reels(Model model, HttpSession session) {
        Object sessionUser = session.getAttribute("user");
        if ("admin".equals(sessionUser))
            return "redirect:/admin";

        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";
        user = userRepository.findById(user.getId()).orElse(user);
        session.setAttribute("user", user);
        session.setAttribute("userId", user.getId());

        populateDashboardCommonModel(model, user);

        List<PostCollaboration> pendingRequests = postCollaborationRepository
                .findByUserAndStatus(user, CollaborationStatus.PENDING);
        model.addAttribute("user", user);
        model.addAttribute("pendingCount", pendingRequests.size());

        model.addAttribute("posts", postRepository.findByPostTypeOrderByCreatedAtDesc("REEL"));
        model.addAttribute("activeCategory", null);
        model.addAttribute("isReelsPage", true);

        return "dashboard";
    }

    private String populateDashboardCommonModel(Model model, User user) {
        List<User> allUsers = userRepository.findAll();
        final User finalUser = user;
        Set<User> following = user.getFollowing();
        List<User> suggestions = allUsers.stream()
                .filter(u -> !u.getId().equals(finalUser.getId()))
                .filter(u -> !following.contains(u))
                .limit(5)
                .collect(Collectors.toList());
        model.addAttribute("suggestions", suggestions);

        Set<Long> requestedUserIds = followRequestRepository.findAll().stream()
                .filter(r -> r.getSender().getId().equals(finalUser.getId()))
                .map(r -> r.getReceiver().getId())
                .collect(Collectors.toSet());
        model.addAttribute("requestedUserIds", requestedUserIds);

        Set<Long> followingUserIds = user.getFollowing().stream()
                .map(User::getId)
                .collect(Collectors.toSet());
        model.addAttribute("followingUserIds", followingUserIds);

        model.addAttribute("notifications", notificationRepository.findByUserOrderByCreatedAtDesc(user));
        model.addAttribute("unreadNotifCount", notificationRepository.countByUserAndIsRead(user, false));

        // Check if current user has a story
        boolean hasStory = !postRepository.findByUserAndPostTypeAndCreatedAtAfterOrderByCreatedAtAsc(
                user, "STORY", java.time.LocalDateTime.now().minusHours(24)).isEmpty();
        model.addAttribute("hasStory", hasStory);

        // Find all users (except self) with active stories
        java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusHours(24);
        List<User> storyUsers = new java.util.ArrayList<>();
        for (User otherUser : allUsers) {
            if (otherUser.getId().equals(user.getId()))
                continue; // skip self
            boolean fHasStory = !postRepository.findByUserAndPostTypeAndCreatedAtAfterOrderByCreatedAtAsc(
                    otherUser, "STORY", cutoff).isEmpty();
            if (fHasStory) {
                storyUsers.add(otherUser);
            }
        }
        model.addAttribute("storyUsers", storyUsers);

        // Fetch active voting polls
        List<Event> votingPolls = eventRepository.findAll().stream()
                .filter(e -> "VOTING".equals(e.getStatus()))
                .filter(e -> e.getVotingEndDate() == null || e.getVotingEndDate().isAfter(LocalDateTime.now()))
                .collect(Collectors.toList());
        model.addAttribute("votingPolls", votingPolls);

        // Ongoing music battles (rooms)
        List<MusicRoom> ongoingBattles = musicRoomRepository.findTop5ByActiveTrueAndPhaseNotOrderByCreatedAtDesc("ENDED");
        model.addAttribute("ongoingMusicBattles", ongoingBattles);

        return "dashboard";
    }

    @GetMapping("/admin")
    public String admin(Model model, HttpSession session) {
        // Only let admin session through
        if (!"admin".equals(session.getAttribute("user")))
            return "redirect:/login";
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("events", eventRepository.findAll());
        model.addAttribute("totalEvents", eventRepository.count());
        model.addAttribute("upcomingEvents", eventRepository.countByStatus("UPCOMING"));
        model.addAttribute("ongoingEvents", eventRepository.countByStatus("ONGOING"));
        model.addAttribute("votingCount", eventRepository.countByStatus("VOTING"));
        model.addAttribute("completedCount", eventRepository.countByStatus("COMPLETED"));
        model.addAttribute("rewardConfig", rewardService.getConfig());
        User user = getUserFromSession(session);
        model.addAttribute("user", user);
        return "admin-dashboard";
    }

    // ── Stub routes: sidebar links that don't have full pages yet ──

    @GetMapping("/explore")
    public String explore(Model model, HttpSession session) {
        if (!isLoggedIn(session))
            return "redirect:/login";

        User currentUser = getUserFromSession(session);
        if (currentUser == null)
            return "redirect:/login";
        final User finalUser = userRepository.findById(currentUser.getId()).orElse(currentUser);

        // All users except self
        List<User> allUsers = userRepository.findAll().stream()
                .filter(u -> !u.getId().equals(finalUser.getId()))
                .collect(Collectors.toList());

        Set<Long> followingUserIds = finalUser.getFollowing().stream()
                .map(User::getId).collect(Collectors.toSet());

        final Long currentUserId = finalUser.getId();
        Set<Long> requestedUserIds = followRequestRepository.findAll().stream()
                .filter(r -> r.getSender().getId().equals(currentUserId))
                .map(r -> r.getReceiver().getId())
                .collect(Collectors.toSet());

        // Follower counts map: userId -> count
        java.util.Map<Long, Integer> followersCount = new java.util.HashMap<>();
        for (User u : allUsers) {
            followersCount.put(u.getId(), u.getFollowers().size());
        }

        model.addAttribute("user", finalUser);
        model.addAttribute("allUsers", allUsers);
        model.addAttribute("followingUserIds", followingUserIds);
        model.addAttribute("requestedUserIds", requestedUserIds);
        model.addAttribute("followersCount", followersCount);
        model.addAttribute("totalPeopleCount", allUsers.size());

        return "explore";
    }

    @GetMapping("/achievements")
    public String achievements(Model model, HttpSession session) {
        if (!isLoggedIn(session))
            return "redirect:/login";

        Object sessionUser = session.getAttribute("user");
        if (sessionUser instanceof User) {
            User u = (User) sessionUser;
            User dbUser = userRepository.findById(u.getId()).orElse(u);
            model.addAttribute("user", dbUser);
            model.addAttribute("isAdmin", false);
            model.addAttribute("attendedCount",
                    eventRegistrationRepository.countByUserAndAttendanceMarked(dbUser, true));
            // Fetch all COMPLETED event registrations with a position
            List<EventRegistration> userAchievements = eventRegistrationRepository.findByUser(dbUser).stream()
                    .filter(r -> r.getPosition() != null && !"Participant".equals(r.getPosition())
                            && !"Absent".equals(r.getPosition()))
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
        if (!isLoggedIn(session))
            return "redirect:/login";
        return "redirect:/dashboard";
    }

    /** True if any valid session (student or admin) exists */
    private boolean isLoggedIn(HttpSession session) {
        Object authUser = httpServletRequest.getAttribute("authenticatedUser");
        if (authUser != null) return true;
        
        Object u = session.getAttribute("user");
        return u instanceof User || "admin".equals(u);
    }
}
