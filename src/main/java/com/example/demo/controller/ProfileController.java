package com.example.demo.controller;

<<<<<<< HEAD
import com.example.demo.model.FollowRequest;
import com.example.demo.model.Notification;
import com.example.demo.model.Post;
import com.example.demo.model.User;
import com.example.demo.repository.FollowRequestRepository;
import com.example.demo.repository.NotificationRepository;
=======
import com.example.demo.model.Post;
import com.example.demo.model.User;
>>>>>>> 48ba3e1bd8a64e1e2f5d106ce5ab45ebc4657ac0
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
<<<<<<< HEAD
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
=======
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
>>>>>>> 48ba3e1bd8a64e1e2f5d106ce5ab45ebc4657ac0

@Controller
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private com.example.demo.repository.PostCollaborationRepository postCollaborationRepository;

<<<<<<< HEAD
    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private FollowRequestRepository followRequestRepository;

    @GetMapping("/{username}")
    public String showPublicProfile(@PathVariable String username, HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return "redirect:/login";
        }

        User targetUser = userRepository.findByUsername(username);
        if (targetUser == null) {
            return "redirect:/dashboard";
        }

        // Refresh current user to get latest following list
        currentUser = userRepository.findById(currentUser.getId()).orElse(currentUser);
        session.setAttribute("user", currentUser);

        boolean isOwnProfile = currentUser.getId().equals(targetUser.getId());
        model.addAttribute("user", targetUser);
        model.addAttribute("isOwnProfile", isOwnProfile);
        model.addAttribute("isFollowing", currentUser.getFollowing().contains(targetUser));

        model.addAttribute("followersCount", targetUser.getFollowers().size());
        model.addAttribute("followingCount", targetUser.getFollowing().size());
        model.addAttribute("followers", targetUser.getFollowers());
        model.addAttribute("following", targetUser.getFollowing());

        // Fetch posts
        List<Post> posts = postRepository.findByUserOrderByCreatedAtDesc(targetUser);
        List<com.example.demo.model.PostCollaboration> collaborations = postCollaborationRepository
                .findByUserAndStatus(targetUser, com.example.demo.model.CollaborationStatus.ACCEPTED);
        for (com.example.demo.model.PostCollaboration col : collaborations) {
            posts.add(col.getPost());
        }
        posts.sort((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()));

        model.addAttribute("posts", posts);
        model.addAttribute("postsCount", posts.size());

        if (isOwnProfile) {
            List<com.example.demo.model.PostCollaboration> pendingRequests = postCollaborationRepository
                    .findByUserAndStatus(currentUser, com.example.demo.model.CollaborationStatus.PENDING);
            model.addAttribute("pendingRequests", pendingRequests);
            model.addAttribute("notifications", notificationRepository.findByUserOrderByCreatedAtDesc(currentUser));
            model.addAttribute("unreadNotifCount", notificationRepository.countByUserAndIsRead(currentUser, false));
            final Long currentUserId = currentUser.getId();
            model.addAttribute("followRequests", followRequestRepository.findAll().stream()
                    .filter(r -> r.getReceiver().getId().equals(currentUserId))
                    .collect(java.util.stream.Collectors.toList()));
        }

        return "profile";
    }

=======
>>>>>>> 48ba3e1bd8a64e1e2f5d106ce5ab45ebc4657ac0
    @GetMapping
    public String showProfile(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
<<<<<<< HEAD
        return showPublicProfile(user.getUsername(), session, model);
    }

    @PostMapping("/update")
    public String updateProfile(@RequestParam String email,
            @RequestParam(required = false) String profilePhotoUrl,
            @RequestParam(required = false) String aboutMe,
            @RequestParam(required = false) String skills,
=======

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
>>>>>>> 48ba3e1bd8a64e1e2f5d106ce5ab45ebc4657ac0
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        User dbUser = userRepository.findById(user.getId()).orElse(null);
        if (dbUser != null) {
<<<<<<< HEAD
            dbUser.setEmail(email);
            if (profilePhotoUrl != null)
                dbUser.setProfilePhotoUrl(profilePhotoUrl);
            if (aboutMe != null)
                dbUser.setAboutMe(aboutMe);
            if (skills != null)
                dbUser.setSkills(skills);
=======
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
>>>>>>> 48ba3e1bd8a64e1e2f5d106ce5ab45ebc4657ac0
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
<<<<<<< HEAD
            @RequestParam(required = false) String collaborators,
=======
>>>>>>> 48ba3e1bd8a64e1e2f5d106ce5ab45ebc4657ac0
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

<<<<<<< HEAD
=======
                // Also save to target to be immediately available without restart in some
                // setups
>>>>>>> 48ba3e1bd8a64e1e2f5d106ce5ab45ebc4657ac0
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
<<<<<<< HEAD
            }
        }

        if (user != null) {
            user = userRepository.findById(user.getId()).orElse(user);
        }

        Post post = new Post(content, user, mediaUrl, mediaType, hashtags);
        postRepository.save(post);

        // Handle collaborators (both mentions and explicit tags)
        Set<User> collaboratorSet = new HashSet<>();

        // 1. Mentions (@username)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("@(\\w+)");
        java.util.regex.Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String uname = matcher.group(1);
            User u = userRepository.findByUsername(uname);
            if (u != null && !u.getId().equals(user.getId())) {
                collaboratorSet.add(u);
            }
        }

        // 2. Explicit collaborators from the UI tagger
        if (collaborators != null && !collaborators.isEmpty()) {
            for (String uname : collaborators.split(",")) {
                User u = userRepository.findByUsername(uname.trim());
                if (u != null && !u.getId().equals(user.getId())) {
                    collaboratorSet.add(u);
                }
            }
        }

        // Save all collaborations
        for (User collabUser : collaboratorSet) {
            com.example.demo.model.PostCollaboration collaboration = new com.example.demo.model.PostCollaboration(
                    post, collabUser, com.example.demo.model.CollaborationStatus.PENDING);
            postCollaborationRepository.save(collaboration);
        }

        return "redirect:/profile";
    }

    @PostMapping("/post/delete/{id}")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> deletePost(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .build();
        }

        Post post = postRepository.findById(id).orElse(null);
        if (post == null) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }

        // Security check: Only the owner can delete
        if (!post.getUser().getId().equals(user.getId())) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .build();
        }

        postRepository.delete(post);
        return org.springframework.http.ResponseEntity.ok().build();
    }

    @Transactional
    @PostMapping("/{id}/follow")
    public String followUser(@PathVariable Long id, HttpSession session,
            @RequestHeader(value = "Referer", required = false) String referer) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null)
            return "redirect:/login";

        User dbTargetUser = userRepository.findById(id).orElse(null);
        User dbCurrentUser = userRepository.findById(currentUser.getId()).orElse(null);

        if (dbCurrentUser != null && dbTargetUser != null
                && !dbTargetUser.getId().equals(dbCurrentUser.getId())) {

            // Check if already following
            if (dbTargetUser.getFollowers().contains(dbCurrentUser)) {
                return (referer != null) ? "redirect:" + referer : "redirect:/profile";
            }

            // Check if request already exists
            if (followRequestRepository.findBySenderAndReceiver(dbCurrentUser, dbTargetUser).isPresent()) {
                return (referer != null) ? "redirect:" + referer : "redirect:/profile";
            }

            // Create FollowRequest
            FollowRequest request = new FollowRequest(dbCurrentUser, dbTargetUser);
            followRequestRepository.save(request);

            // Create Notification
            Notification notif = new Notification(dbTargetUser, dbCurrentUser,
                    "@" + dbCurrentUser.getUsername() + " wants to follow you!", "FOLLOW_REQUEST");
            notificationRepository.save(notif);
        }
        return (referer != null) ? "redirect:" + referer : "redirect:/profile";
    }

    @Transactional
    @PostMapping("/{id}/unfollow")
    public String unfollowUser(@PathVariable Long id, HttpSession session,
            @RequestHeader(value = "Referer", required = false) String referer) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null)
            return "redirect:/login";

        User dbTargetUser = userRepository.findById(id).orElse(null);
        User dbCurrentUser = userRepository.findById(currentUser.getId()).orElse(null);

        if (dbCurrentUser != null && dbTargetUser != null) {
            dbTargetUser.getFollowers().remove(dbCurrentUser);
            userRepository.save(dbTargetUser);
            // Refresh session
            dbCurrentUser = userRepository.findById(currentUser.getId()).orElse(dbCurrentUser);
            session.setAttribute("user", dbCurrentUser);
        }
        return (referer != null) ? "redirect:" + referer : "redirect:/profile";
    }

    @GetMapping("/api/users/search")
    @ResponseBody
    public List<Map<String, Object>> searchUsers(@RequestParam String q) {
        List<User> users = userRepository.findAll(); // Simple search for demo
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (User u : users) {
            if (u.getUsername().toLowerCase().contains(q.toLowerCase())) {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("username", u.getUsername());
                map.put("profilePhotoUrl", u.getProfilePhotoUrl());
                result.add(map);
            }
        }
        return result;
    }

    @PostMapping("/collaboration/{id}/accept")
    public String acceptCollaboration(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null)
            return "redirect:/login";
=======
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
>>>>>>> 48ba3e1bd8a64e1e2f5d106ce5ab45ebc4657ac0

        com.example.demo.model.PostCollaboration collaboration = postCollaborationRepository.findById(id).orElse(null);
        if (collaboration != null && collaboration.getUser().getId().equals(user.getId())) {
            collaboration.setStatus(com.example.demo.model.CollaborationStatus.ACCEPTED);
            postCollaborationRepository.save(collaboration);
        }
<<<<<<< HEAD
=======

>>>>>>> 48ba3e1bd8a64e1e2f5d106ce5ab45ebc4657ac0
        return "redirect:/profile";
    }

    @PostMapping("/collaboration/{id}/reject")
<<<<<<< HEAD
    public String rejectCollaboration(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null)
            return "redirect:/login";
=======
    public String rejectCollaboration(@org.springframework.web.bind.annotation.PathVariable Long id,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
>>>>>>> 48ba3e1bd8a64e1e2f5d106ce5ab45ebc4657ac0

        com.example.demo.model.PostCollaboration collaboration = postCollaborationRepository.findById(id).orElse(null);
        if (collaboration != null && collaboration.getUser().getId().equals(user.getId())) {
            collaboration.setStatus(com.example.demo.model.CollaborationStatus.REJECTED);
            postCollaborationRepository.save(collaboration);
        }
<<<<<<< HEAD
        return "redirect:/profile";
    }

    @Transactional
    @PostMapping("/notifications/mark-all-read")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> markAllNotificationsRead(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return org.springframework.http.ResponseEntity.status(401).build();
        }
        User dbUser = userRepository.findById(user.getId()).orElse(null);
        if (dbUser != null) {
            List<Notification> unread = notificationRepository.findByUserAndIsReadOrderByCreatedAtDesc(dbUser, false);
            for (Notification n : unread) {
                n.setRead(true);
            }
            notificationRepository.saveAll(unread);
        }
        return org.springframework.http.ResponseEntity.ok().build();
    }

    @Transactional
    @PostMapping("/follow-request/{id}/accept")
    public String acceptFollow(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null)
            return "redirect:/login";

        FollowRequest fr = followRequestRepository.findById(id).orElse(null);
        if (fr != null && fr.getReceiver().getId().equals(user.getId())) {
            User sender = fr.getSender();
            User receiver = fr.getReceiver();

            receiver.getFollowers().add(sender);
            userRepository.save(receiver);

            followRequestRepository.delete(fr);

            // Notification for the sender that they were accepted
            notificationRepository.save(new Notification(sender, receiver,
                    "@" + receiver.getUsername() + " accepted your follow request!", "FOLLOW_ACCEPT"));
        }
        return "redirect:/profile";
    }

    @Transactional
    @PostMapping("/follow-request/{id}/reject")
    public String rejectFollow(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null)
            return "redirect:/login";

        FollowRequest fr = followRequestRepository.findById(id).orElse(null);
        if (fr != null && fr.getReceiver().getId().equals(user.getId())) {
            followRequestRepository.delete(fr);
        }
=======

>>>>>>> 48ba3e1bd8a64e1e2f5d106ce5ab45ebc4657ac0
        return "redirect:/profile";
    }
}
