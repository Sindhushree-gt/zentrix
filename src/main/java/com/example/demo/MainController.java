package com.example.demo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

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
        model.addAttribute("users", userRepository.findAll());
        return "admin-dashboard";
    }
}
