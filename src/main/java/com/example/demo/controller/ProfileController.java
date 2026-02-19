package com.example.demo.controller;

import com.example.demo.model.Post;
import com.example.demo.model.User;
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private com.example.demo.repository.PostCollaborationRepository postCollaborationRepository;

    @GetMapping
    public String showProfile(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        // Refresh user data from DB
        user = userRepository.findById(user.getId()).orElse(user);

        model.addAttribute("user", user);

        // Fetch posts where user is owner
        java.util.List<Post> myPosts = postRepository.findByUserOrderByCreatedAtDesc(user);

        // Fetch posts where user is a collaborator (ACCEPTED)
        java.util.List<com.example.demo.model.PostCollaboration> collaborations = postCollaborationRepository
                .findByUserAndStatus(user, com.example.demo.model.CollaborationStatus.ACCEPTED);
        for (com.example.demo.model.PostCollaboration col : collaborations) {
            myPosts.add(col.getPost());
        }

        // Sort combinad list by createdAt desc
        myPosts.sort((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()));

        model.addAttribute("posts", myPosts);

        // Fetch pending collaboration requests
        java.util.List<com.example.demo.model.PostCollaboration> pendingRequests = postCollaborationRepository
                .findByUserAndStatus(user, com.example.demo.model.CollaborationStatus.PENDING);
        model.addAttribute("pendingRequests", pendingRequests);

        return "profile";
    }

    @PostMapping("/update")
    public String updateProfile(@RequestParam String username,
            @RequestParam String email,
            @RequestParam String dob,
            @RequestParam String gender,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        User dbUser = userRepository.findById(user.getId()).orElse(null);
        if (dbUser != null) {
            dbUser.setUsername(username);
            dbUser.setEmail(email);
            // Assuming dob is stored as String for now based on previous code or convert if
            // needed
            // If User entity has LocalDate, we need to parse. Based on register.html input
            // type="date", it sends a string.
            // Let's assume User entity has LocalDate and we need to parse it.
            // Checking User entity... it has LocalDate dob.
            // So we need to parse the string to LocalDate.
            if (dob != null && !dob.isEmpty()) {
                dbUser.setDob(java.time.LocalDate.parse(dob));
            }
            dbUser.setGender(gender);
            userRepository.save(dbUser);
            session.setAttribute("user", dbUser);
        }
        return "redirect:/profile?success";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String newPassword, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        User dbUser = userRepository.findById(user.getId()).orElse(null);
        if (dbUser != null) {
            dbUser.setPassword(newPassword);
            userRepository.save(dbUser);
        }
        return "redirect:/profile?passwordReset";
    }

    @PostMapping("/post")
    public String createPost(@RequestParam String content,
            @RequestParam(required = false) org.springframework.web.multipart.MultipartFile file,
            @RequestParam(required = false) String hashtags,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        String mediaUrl = null;
        String mediaType = null;

        if (file != null && !file.isEmpty()) {
            try {
                String fileName = java.util.UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                String uploadDir = "src/main/resources/static/uploads/";
                java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);
                if (!java.nio.file.Files.exists(uploadPath)) {
                    java.nio.file.Files.createDirectories(uploadPath);
                }

                // Also save to target to be immediately available without restart in some
                // setups
                String targetUploadDir = "target/classes/static/uploads/";
                java.nio.file.Path targetUploadPath = java.nio.file.Paths.get(targetUploadDir);
                if (!java.nio.file.Files.exists(targetUploadPath)) {
                    java.nio.file.Files.createDirectories(targetUploadPath);
                }

                java.nio.file.Files.copy(file.getInputStream(), uploadPath.resolve(fileName),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                java.nio.file.Files.copy(file.getInputStream(), targetUploadPath.resolve(fileName),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                mediaUrl = "/uploads/" + fileName;
                String contentType = file.getContentType();
                if (contentType != null && contentType.startsWith("video")) {
                    mediaType = "VIDEO";
                } else {
                    mediaType = "IMAGE";
                }
            } catch (java.io.IOException e) {
                e.printStackTrace();
                // Handle error
            }
        }

        Post post = new Post(content, user, mediaUrl, mediaType, hashtags);
        postRepository.save(post);

        // Handle mentions for collaboration
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("@(\\w+)");
        java.util.regex.Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String username = matcher.group(1);
            User mentionedUser = userRepository.findByUsername(username);
            if (mentionedUser != null && !mentionedUser.getId().equals(user.getId())) {
                com.example.demo.model.PostCollaboration collaboration = new com.example.demo.model.PostCollaboration(
                        post, mentionedUser, com.example.demo.model.CollaborationStatus.PENDING);
                postCollaborationRepository.save(collaboration);
            }
        }

        return "redirect:/profile";
    }

    @PostMapping("/collaboration/{id}/accept")
    public String acceptCollaboration(@org.springframework.web.bind.annotation.PathVariable Long id,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        com.example.demo.model.PostCollaboration collaboration = postCollaborationRepository.findById(id).orElse(null);
        if (collaboration != null && collaboration.getUser().getId().equals(user.getId())) {
            collaboration.setStatus(com.example.demo.model.CollaborationStatus.ACCEPTED);
            postCollaborationRepository.save(collaboration);
        }

        return "redirect:/profile";
    }

    @PostMapping("/collaboration/{id}/reject")
    public String rejectCollaboration(@org.springframework.web.bind.annotation.PathVariable Long id,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        com.example.demo.model.PostCollaboration collaboration = postCollaborationRepository.findById(id).orElse(null);
        if (collaboration != null && collaboration.getUser().getId().equals(user.getId())) {
            collaboration.setStatus(com.example.demo.model.CollaborationStatus.REJECTED);
            postCollaborationRepository.save(collaboration);
        }

        return "redirect:/profile";
    }
}
