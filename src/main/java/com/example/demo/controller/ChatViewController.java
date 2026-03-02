package com.example.demo.controller;

import com.example.demo.model.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatViewController {

    @GetMapping("/messages")
    public String messagesPage(HttpSession session, Model model) {
        Object sessionUser = session.getAttribute("user");
        if (!(sessionUser instanceof User)) {
            return "redirect:/login";
        }
        User user = (User) sessionUser;
        model.addAttribute("currentUser", user);
        return "messages";
    }
}
