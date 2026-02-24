package com.example.demo;

<<<<<<< HEAD
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
=======
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
>>>>>>> 48ba3e1bd8a64e1e2f5d106ce5ab45ebc4657ac0

@Controller
public class MainController {

<<<<<<< HEAD
    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostCollaborationRepository postCollaborationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

=======
>>>>>>> 48ba3e1bd8a64e1e2f5d106ce5ab45ebc4657ac0
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

    @GetMapping("/events")
    public String events() {
        return "events";
    }

    @GetMapping("/games")
    public String games() {
        return "games";
    }

<<<<<<< HEAD
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
=======
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.demo.repository.PostRepository postRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private com.example.demo.repository.PostCollaborationRepository postCollaborationRepository;

    @GetMapping("/dashboard")
    public String dashboard(org.springframework.ui.Model model, jakarta.servlet.http.HttpSession session) {
        Object sessionUser = session.getAttribute("user");
        if (sessionUser instanceof com.example.demo.model.User) {
            com.example.demo.model.User user = (com.example.demo.model.User) sessionUser;
            java.util.List<com.example.demo.model.PostCollaboration> pendingRequests = postCollaborationRepository
                    .findByUserAndStatus(user, com.example.demo.model.CollaborationStatus.PENDING);
            model.addAttribute("pendingCount", pendingRequests.size());
        }
        model.addAttribute("posts", postRepository.findAllByOrderByCreatedAtDesc());
        return "dashboard";
    }

    @org.springframework.beans.factory.annotation.Autowired
    private com.example.demo.repository.UserRepository userRepository;

    @GetMapping("/admin")
    public String admin(org.springframework.ui.Model model) {
>>>>>>> 48ba3e1bd8a64e1e2f5d106ce5ab45ebc4657ac0
        model.addAttribute("users", userRepository.findAll());
        return "admin-dashboard";
    }
}
