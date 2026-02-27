package com.example.demo;

import com.example.demo.model.User;
import com.example.demo.model.PostCollaboration;
import com.example.demo.model.CollaborationStatus;
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.PostCollaborationRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.NotificationRepository;
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

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostCollaborationRepository postCollaborationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

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
        if (!(sessionUser instanceof User)) {
            return "redirect:/login";
        }
        User user = (User) sessionUser;
        // Refresh from DB
        user = userRepository.findById(user.getId()).orElse(user);
        session.setAttribute("user", user);

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
    public String admin(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "admin-dashboard";
    }
}
