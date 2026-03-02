package com.example.demo.controller;

import com.example.demo.model.Event;
import com.example.demo.model.EventRegistration;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.EventRepository;
import com.example.demo.repository.EventRegistrationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/events")
public class EventController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventRegistrationRepository eventRegistrationRepository;

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

    @GetMapping
    public String listEvents(@RequestParam(required = false) String category, Model model, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) {
            return "redirect:/login";
        }
        // Refresh
        user = userRepository.findById(user.getId()).orElse(user);
        session.setAttribute("user", user);
        session.setAttribute("userId", user.getId());

        List<Event> events;
        if (category != null && !category.isEmpty() && !category.equalsIgnoreCase("All")) {
            events = eventRepository.findByCategory(category);
        } else {
            events = eventRepository.findAll();
        }

        model.addAttribute("events", events);
        model.addAttribute("activeCategory", category != null ? category : "All");
        model.addAttribute("user", user);
        return "events";
    }

    @GetMapping("/{id}")
    public String eventDetails(@PathVariable Long id, Model model, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) {
            return "redirect:/login";
        }
        // Refresh
        user = userRepository.findById(user.getId()).orElse(user);
        session.setAttribute("user", user);
        session.setAttribute("userId", user.getId());

        Event event = eventRepository.findById(id).orElse(null);
        if (event == null)
            return "redirect:/events";

        boolean isRegistered = eventRegistrationRepository.findByEventAndUser(event, user).isPresent();
        long registrationCount = eventRegistrationRepository.countByEvent(event);

        model.addAttribute("event", event);
        model.addAttribute("user", user);
        model.addAttribute("isRegistered", isRegistered);
        model.addAttribute("registrationCount", registrationCount);
        return "event-registration";
    }

    @PostMapping("/{id}/register")
    public String registerForEvent(@PathVariable Long id, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) {
            return "redirect:/login";
        }
        // Refresh
        user = userRepository.findById(user.getId()).orElse(user);
        session.setAttribute("user", user);
        session.setAttribute("userId", user.getId());

        Event event = eventRepository.findById(id).orElse(null);
        if (event == null)
            return "redirect:/events";

        if (eventRegistrationRepository.findByEventAndUser(event, user).isEmpty()) {
            EventRegistration registration = new EventRegistration();
            registration.setEvent(event);
            registration.setUser(user);
            eventRegistrationRepository.save(registration);
        }

        return "redirect:/events/" + id + "?success=true";
    }
}
