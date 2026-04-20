package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.RewardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import com.example.demo.config.JwtUtil;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RewardService rewardService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") User user, Model model) {
        if (userRepository.findByUsername(user.getUsername()) != null) {
            return "redirect:/register?error=duplicate";
        }
        userRepository.save(user);
        return "redirect:/home";
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String loginUser(@org.springframework.web.bind.annotation.RequestParam String username,
            @org.springframework.web.bind.annotation.RequestParam String password,
            jakarta.servlet.http.HttpSession session,
            jakarta.servlet.http.HttpServletResponse response) {
        if ("admin".equals(username) && "admin123".equals(password)) {
            String token = jwtUtil.generateToken("admin");
            jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("jwtToken", token);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            response.addCookie(cookie);
            
            session.setAttribute("user", "admin");
            return "redirect:/admin?auth=" + token;
        }
        User user = userRepository.findByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            rewardService.awardDailyLogin(user); // Zen Coins Awarded here
            
            String token = jwtUtil.generateToken(username);
            jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("jwtToken", token);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            response.addCookie(cookie);
            
            session.setAttribute("user", user);
            session.setAttribute("userId", user.getId());
            return "redirect:/dashboard?auth=" + token;
        } else {
            return "redirect:/login?error";
        }
    }

    @GetMapping("/logout")
    public String logout(jakarta.servlet.http.HttpSession session, jakarta.servlet.http.HttpServletResponse response) {
        session.invalidate();
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("jwtToken", null);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return "redirect:/login";
    }
}
