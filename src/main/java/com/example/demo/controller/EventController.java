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
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.net.InetAddress;

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

    // ─────────────────────────────────────────────────────────
    //  PUBLIC: List Events  (Student View)
    // ─────────────────────────────────────────────────────────
    @GetMapping
    public String listEvents(@RequestParam(required = false) String category, Model model, HttpSession session) {
        User user = getUserFromSession(session);
        boolean adminViewing = isAdmin(session);
        if (user != null) {
            user = userRepository.findById(user.getId()).orElse(user);
            session.setAttribute("user", user);
            session.setAttribute("userId", user.getId());
        }

        // Redirect to login only if neither student nor admin
        if (user == null && !adminViewing) return "redirect:/login";

        // All events (filtered by category if given)
        List<Event> allEvents;
        if (category != null && !category.isEmpty() && !category.equalsIgnoreCase("All")) {
            allEvents = eventRepository.findByCategory(category);
        } else {
            allEvents = eventRepository.findAll();
        }

        // Trending = first 3 UPCOMING events
        List<Event> trending = eventRepository.findByStatus("UPCOMING")
                .stream().limit(3).collect(Collectors.toList());

        // Upcoming = all UPCOMING events
        List<Event> upcoming = eventRepository.findByStatus("UPCOMING");

        model.addAttribute("events", allEvents);
        model.addAttribute("trending", trending);
        model.addAttribute("upcoming", upcoming);
        model.addAttribute("activeCategory", category != null ? category : "All");
        model.addAttribute("user", user);
        model.addAttribute("isAdmin", adminViewing);
        return "events";
    }

    // ─────────────────────────────────────────────────────────
    //  PUBLIC: Event Detail Page
    // ─────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public String eventDetails(@PathVariable Long id, Model model, HttpSession session) {
        User user = getUserFromSession(session);
        boolean adminViewing = isAdmin(session);

        // Allow access if either a student or admin is logged in
        if (user == null && !adminViewing) return "redirect:/login";

        if (user != null) {
            user = userRepository.findById(user.getId()).orElse(user);
            session.setAttribute("user", user);
            session.setAttribute("userId", user.getId());
        }

        Event event = eventRepository.findById(id).orElse(null);
        if (event == null) return "redirect:/events";

        EventRegistration userReg = null;
        if (user != null) {
            userReg = eventRegistrationRepository.findByEventAndUser(event, user).orElse(null);
            
            // Fix for missing ticket IDs in legacy records
            if (userReg != null && (userReg.getTicketId() == null || userReg.getTicketId().isBlank())) {
                userReg.setTicketId(generateTicketId(event));
                eventRegistrationRepository.save(userReg);
            }
        }
        boolean isRegistered = (userReg != null);
        long dbRegistrationCount = eventRegistrationRepository.countByEvent(event);
        long fixedCount = (event.getFixedParticipants() != null) ? event.getFixedParticipants() : 0;
        long registrationCount = dbRegistrationCount + fixedCount;
        
        // Use -1 for unlimited spots
        int spotsLeft = (event.getMaxParticipants() != null && event.getMaxParticipants() > 0)
                ? Math.max(0, event.getMaxParticipants() - (int) registrationCount) : -1;

        // Related events (same category, exclude current)
        List<Event> related = eventRepository.findByCategory(event.getCategory())
                .stream().filter(e -> !e.getId().equals(id)).limit(3).collect(Collectors.toList());

        // For Secret Voting System (when event is ONGOING)
        boolean hasVoted = false;
        List<EventRegistration> candidates = new java.util.ArrayList<>();
        if ("ONGOING".equals(event.getStatus()) || "COMPLETED".equals(event.getStatus())) {
            if (user != null) {
                User dbUser = userRepository.findById(user.getId()).orElse(user);
                hasVoted = dbUser.getVotedEvents().contains(id);
            }
            candidates = eventRegistrationRepository.findByEvent(event).stream()
                    .filter(EventRegistration::isAttendanceMarked)
                    .collect(Collectors.toList());
        }

        model.addAttribute("event", event);
        model.addAttribute("user", user);
        model.addAttribute("isAdmin", adminViewing);
        model.addAttribute("isRegistered", isRegistered);
        model.addAttribute("userReg", userReg);
        model.addAttribute("registrationCount", registrationCount);
        model.addAttribute("spotsLeft", spotsLeft);
        model.addAttribute("related", related);
        model.addAttribute("hasVoted", hasVoted);
        model.addAttribute("candidates", candidates);
        return "event-registration";
    }

    // ─────────────────────────────────────────────────────────
    //  PUBLIC VOTING
    // ─────────────────────────────────────────────────────────
    @PostMapping("/{id}/vote/{regId}")
    public String castVote(@PathVariable Long id, @PathVariable Long regId, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";

        Event event = eventRepository.findById(id).orElse(null);
        if (event == null || !"ONGOING".equals(event.getStatus())) return "redirect:/events/" + id;

        User dbUser = userRepository.findById(user.getId()).orElse(null);
        if (dbUser == null) return "redirect:/login";

        if (dbUser.getVotedEvents().contains(id)) {
            return "redirect:/events/" + id + "?alreadyVoted=true";
        }

        EventRegistration reg = eventRegistrationRepository.findById(regId).orElse(null);
        if (reg != null && reg.getEvent().getId().equals(id)) {
            reg.setPublicVotes(reg.getPublicVotes() + 1);
            eventRegistrationRepository.save(reg);

            dbUser.getVotedEvents().add(id);
            userRepository.save(dbUser);
        }

        return "redirect:/events/" + id + "?voteSuccess=true";
    }

    // ─────────────────────────────────────────────────────────
    //  REGISTER: Entry point — decides Free vs Paid
    // ─────────────────────────────────────────────────────────
    @PostMapping("/{id}/register")
    public String initiateRegistration(
            @PathVariable Long id,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String college,
            @RequestParam(required = false) String yearOfStudy,
            HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";

        Event event = eventRepository.findById(id).orElse(null);
        if (event == null) return "redirect:/events";

        if (eventRegistrationRepository.findByEventAndUser(event, user).isPresent()) {
            return "redirect:/events/" + id + "?alreadyRegistered=true";
        }

        // Store reg info in session temporarily for paid flow
        session.setAttribute("regFullName", fullName);
        session.setAttribute("regEmail", email);
        session.setAttribute("regPhone", phone);
        session.setAttribute("regCollege", college);
        session.setAttribute("regYear", yearOfStudy);

        if ("Paid".equalsIgnoreCase(event.getEntryFeeType())) {
            return "redirect:/events/" + id + "/payment";
        } else {
            return completeRegistration(event, user, "FREE", fullName, email, phone, college, yearOfStudy);
        }
    }

    // ─────────────────────────────────────────────────────────
    //  PAYMENT: Show payment gateway page
    // ─────────────────────────────────────────────────────────
    @GetMapping("/{id}/payment")
    public String showPaymentPage(@PathVariable Long id, Model model, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";

        Event event = eventRepository.findById(id).orElse(null);
        if (event == null) return "redirect:/events";

        if (eventRegistrationRepository.findByEventAndUser(event, user).isPresent()) {
            return "redirect:/events/" + id + "?alreadyRegistered=true";
        }

        model.addAttribute("event", event);
        model.addAttribute("user", user);
        return "payment";
    }

    // ─────────────────────────────────────────────────────────
    //  PAYMENT: Process payment (simulate success)
    // ─────────────────────────────────────────────────────────
    @PostMapping("/{id}/payment/confirm")
    public String confirmPayment(
            @PathVariable Long id,
            @RequestParam(required = false) String cardName,
            @RequestParam(required = false) String cardNumber,
            HttpSession session) {

        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";

        Event event = eventRepository.findById(id).orElse(null);
        if (event == null) return "redirect:/events";

        if (eventRegistrationRepository.findByEventAndUser(event, user).isPresent()) {
            return "redirect:/events/" + id + "?alreadyRegistered=true";
        }

        // Retrieve registration info stored in session
        String fullName    = (String) session.getAttribute("regFullName");
        String email       = (String) session.getAttribute("regEmail");
        String phone       = (String) session.getAttribute("regPhone");
        String college     = (String) session.getAttribute("regCollege");
        String yearOfStudy = (String) session.getAttribute("regYear");

        return completeRegistration(event, user, "PAID", fullName, email, phone, college, yearOfStudy);
    }

    // ─────────────────────────────────────────────────────────
    //  TICKET: Show confirmation + ticket
    // ─────────────────────────────────────────────────────────
    @GetMapping("/ticket/{ticketId}")
    public String showTicket(@PathVariable String ticketId, Model model, HttpSession session, HttpServletRequest request) {
        User user = getUserFromSession(session);
        boolean adminViewing = isAdmin(session);
        
        // Publicly accessible for QR code verification
        // (Previously restricted to logged-in users only)

        EventRegistration reg = eventRegistrationRepository.findByTicketId(ticketId).orElse(null);
        if (reg == null) return "redirect:/events";

        // Construct full URL and a text summary for the QR code
        String serverName = request.getServerName();
        String port = String.valueOf(request.getServerPort());
        
        // Localhost Fix: Try to get actual IP if running locally so phone can scan
        if (serverName.equals("localhost") || serverName.equals("127.0.0.1")) {
            try {
                serverName = InetAddress.getLocalHost().getHostAddress();
            } catch (Exception e) {
                // fallback to localhost if IP detection fails
            }
        }
        
        String baseUrl = request.getScheme() + "://" + serverName + ":" + port;
        String fullUrl = baseUrl + "/events/ticket/" + ticketId;
        
        // Formatted summary for the scanner
        String qrData = "ZENTRIX VERIFIED TICKET\n" +
                        "----------------------\n" +
                        "ID: " + ticketId + "\n" +
                        "NAME: " + reg.getFullName() + "\n" +
                        "EVENT: " + reg.getEvent().getTitle() + "\n" +
                        "STATUS: " + reg.getRegistrationStatus() + "\n" +
                        "----------------------\n" +
                        "Link: " + fullUrl;
        
        model.addAttribute("ticketUrl", qrData);

        model.addAttribute("registration", reg);
        model.addAttribute("event", reg.getEvent());
        model.addAttribute("user", reg.getUser() != null ? reg.getUser() : user);
        model.addAttribute("isAdmin", adminViewing);
        return "ticket";
    }

    // ─────────────────────────────────────────────────────────
    //  ADMIN: Manage Events
    // ─────────────────────────────────────────────────────────
    @GetMapping("/admin/manage")
    public String adminManageEvents(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";

        List<Event> events = eventRepository.findAll();
        model.addAttribute("events", events);
        model.addAttribute("totalEvents", events.size());
        model.addAttribute("upcomingCount", eventRepository.countByStatus("UPCOMING"));
        model.addAttribute("ongoingCount", eventRepository.countByStatus("ONGOING"));
        model.addAttribute("completedCount", eventRepository.countByStatus("COMPLETED"));
        return "admin-events";
    }

    @GetMapping("/admin/create")
    public String showCreateEventForm(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        model.addAttribute("event", new Event());
        return "admin-create-event";
    }

    @PostMapping("/admin/create")
    public String createEvent(
            @RequestParam String title,
            @RequestParam String category,
            @RequestParam String description,
            @RequestParam String dateTime,
            @RequestParam String venue,
            @RequestParam String entryFeeType,
            @RequestParam(required = false) String price,
            @RequestParam Integer maxParticipants,
            @RequestParam(required = false, defaultValue = "0") Integer fixedParticipants,
            @RequestParam(required = false, defaultValue = "Offline") String eventMode,
            @RequestParam(required = false) String meetingLink,
            @RequestParam(required = false) String imageUrl,
            HttpSession session) {

        if (!isAdmin(session)) return "redirect:/login";

        Event event = new Event();
        event.setTitle(title);
        event.setCategory(category);
        event.setDescription(description);
        event.setVenue(venue);
        event.setEntryFeeType(entryFeeType);
        event.setMaxParticipants(maxParticipants);
        event.setFixedParticipants(fixedParticipants);
        event.setStatus("UPCOMING");
        event.setOrganizer("Zentrix Admin");
        event.setEventMode(eventMode);
        event.setMeetingLink(meetingLink);

        if ("Free".equals(entryFeeType)) {
            event.setPrice("Free");
        } else {
            event.setPrice(price != null && !price.isBlank() ? "\u20b9" + price : "Paid");
        }

        try {
            event.setDateTime(LocalDateTime.parse(dateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
        } catch (Exception ignored) {}

        // Use provided URL or fallback to default category image
        if (imageUrl != null && !imageUrl.isBlank()) {
            event.setImageUrl(imageUrl);
        } else {
            event.setImageUrl(getDefaultImage(category));
        }

        eventRepository.save(event);
        return "redirect:/events/admin/manage?created=true";
    }

    @GetMapping("/admin/edit/{id}")
    public String showEditEventForm(@PathVariable Long id, Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        Event event = eventRepository.findById(id).orElse(null);
        if (event == null) return "redirect:/events/admin/manage";
        model.addAttribute("event", event);
        return "admin-create-event";
    }

    @PostMapping("/admin/edit/{id}")
    public String updateEvent(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam String category,
            @RequestParam String description,
            @RequestParam String dateTime,
            @RequestParam String venue,
            @RequestParam String entryFeeType,
            @RequestParam(required = false) String price,
            @RequestParam Integer maxParticipants,
            @RequestParam(required = false, defaultValue = "0") Integer fixedParticipants,
            @RequestParam(required = false, defaultValue = "Offline") String eventMode,
            @RequestParam(required = false) String meetingLink,
            @RequestParam String status,
            @RequestParam(required = false) String imageUrl,
            HttpSession session) {

        if (!isAdmin(session)) return "redirect:/login";
        Event event = eventRepository.findById(id).orElse(null);
        if (event == null) return "redirect:/events/admin/manage";

        event.setTitle(title);
        event.setCategory(category);
        event.setDescription(description);
        event.setVenue(venue);
        event.setEntryFeeType(entryFeeType);
        event.setMaxParticipants(maxParticipants);
        event.setFixedParticipants(fixedParticipants);
        event.setStatus(status);
        event.setEventMode(eventMode);
        event.setMeetingLink(meetingLink);
        
        if ("Free".equals(entryFeeType)) {
            event.setPrice("Free");
        } else {
            event.setPrice(price != null && !price.isBlank() ? "\u20b9" + price : "Paid");
        }

        try {
            event.setDateTime(LocalDateTime.parse(dateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
        } catch (Exception ignored) {}

        // Update imageUrl if a new one is provided
        if (imageUrl != null && !imageUrl.isBlank()) {
            event.setImageUrl(imageUrl);
        }
        // else keep existing imageUrl unchanged

        eventRepository.save(event);
        return "redirect:/events/admin/manage?updated=true";
    }

    @PostMapping("/admin/delete/{id}")
    public String deleteEvent(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        eventRepository.deleteById(id);
        return "redirect:/events/admin/manage?deleted=true";
    }

    // ─────────────────────────────────────────────────────────
    //  ADMIN: Attendance Dashboard
    // ─────────────────────────────────────────────────────────
    @GetMapping("/admin/{id}/attendance")
    public String adminAttendancePage(@PathVariable Long id, Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";

        Event event = eventRepository.findById(id).orElse(null);
        if (event == null) return "redirect:/events/admin/manage";

        List<EventRegistration> registrations = eventRegistrationRepository.findByEvent(event);
        long attendedCount = eventRegistrationRepository.countByEventAndAttendanceMarked(event, true);

        model.addAttribute("event", event);
        model.addAttribute("registrations", registrations);
        model.addAttribute("attendedCount", attendedCount);
        model.addAttribute("totalCount", registrations.size());
        return "admin-attendance";
    }

    /** Mark event as ONGOING */
    @PostMapping("/admin/{id}/start")
    public String startEvent(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        Event event = eventRepository.findById(id).orElse(null);
        if (event != null) {
            event.setStatus("ONGOING");
            eventRepository.save(event);
        }
        return "redirect:/events/admin/" + id + "/attendance?started=true";
    }

    /** Mark event as COMPLETED and Assign XP/Results */
    @PostMapping("/admin/{id}/complete")
    public String completeEvent(
            @PathVariable Long id,
            @RequestParam(required = false) List<Long> winnerIds,
            @RequestParam(required = false) List<Long> runnerIds,
            @RequestParam(defaultValue = "100") int winnerPoints,
            @RequestParam(defaultValue = "50") int runnerPoints,
            @RequestParam(defaultValue = "10") int defaultPoints,
            HttpSession session,
            jakarta.servlet.http.HttpServletRequest request) {
        if (!isAdmin(session)) return "redirect:/login";

        Event event = eventRepository.findById(id).orElse(null);
        if (event == null) return "redirect:/events/admin/manage";

        event.setStatus("COMPLETED");
        eventRepository.save(event);

        List<EventRegistration> registrations = eventRegistrationRepository.findByEvent(event);

        // Find max public votes for fair 40% weighting algorithm
        int maxVotes = 0;
        for (EventRegistration r : registrations) {
            if (r.isAttendanceMarked() && r.getPublicVotes() != null && r.getPublicVotes() > maxVotes) {
                maxVotes = r.getPublicVotes();
            }
        }

        // Process results for all registrations
        for (EventRegistration reg : registrations) {
            String position = "Participant";
            int points = 0;

            if (reg.isAttendanceMarked()) {
                // Secret Voting Weight System
                String jScoreStr = request.getParameter("judgeScore_" + reg.getId());
                double jScore = 0.0;
                if (jScoreStr != null && !jScoreStr.isEmpty()) {
                    try { jScore = Double.parseDouble(jScoreStr); } catch (Exception ignored) {}
                }
                reg.setJudgeScore(jScore);
                
                int pVotes = reg.getPublicVotes() != null ? reg.getPublicVotes() : 0;
                double publicScore = maxVotes > 0 ? ((double) pVotes / maxVotes) * 100.0 : 0.0;
                double finalScore = (jScore * 0.6) + (publicScore * 0.4);
                reg.setFinalScore(finalScore);

                // Assign Rank
                if (winnerIds != null && winnerIds.contains(reg.getId())) {
                    position = "Winner";
                    points = winnerPoints;
                } else if (runnerIds != null && runnerIds.contains(reg.getId())) {
                    position = "Runner";
                    points = runnerPoints;
                } else {
                    points = defaultPoints; // Basic participation points
                }

                reg.setPosition(position);
                reg.setPointsEarned(points);

                // Update User XP
                User u = reg.getUser();
                if (u != null) {
                    u.setXp((u.getXp() == null ? 0 : u.getXp()) + points);
                    u.setLevel(calculateLevel(u.getXp()));
                    userRepository.save(u);
                }
            } else {
                reg.setPosition("Absent");
                reg.setPointsEarned(0);
            }
        }
        eventRegistrationRepository.saveAll(registrations);

        return "redirect:/events/admin/" + id + "/attendance?completed=true";
    }

    private String calculateLevel(int xp) {
        if (xp >= 1000) return "Platinum";
        if (xp >= 500) return "Gold";
        if (xp >= 200) return "Silver";
        if (xp >= 50) return "Bronze";
        return "Novice";
    }

    /** Mark attendance for a specific ticket ID (offline scan) */
    @PostMapping("/admin/{id}/attendance/mark")
    public String markAttendance(@PathVariable Long id,
                                  @RequestParam String ticketId,
                                  HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";

        EventRegistration reg = eventRegistrationRepository.findByTicketId(ticketId.trim().toUpperCase()).orElse(null);
        if (reg == null || !reg.getEvent().getId().equals(id)) {
            return "redirect:/events/admin/" + id + "/attendance?error=invalid";
        }
        if (reg.isAttendanceMarked()) {
            return "redirect:/events/admin/" + id + "/attendance?error=already";
        }
        reg.setAttendanceMarked(true);
        reg.setAttendedAt(LocalDateTime.now());
        eventRegistrationRepository.save(reg);
        return "redirect:/events/admin/" + id + "/attendance?marked=" + ticketId;
    }

    /** Mark ALL registered participants as attended */
    @PostMapping("/admin/{id}/attendance/mark-all")
    public String markAllAttendance(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        Event event = eventRepository.findById(id).orElse(null);
        if (event != null) {
            List<EventRegistration> regs = eventRegistrationRepository.findByEvent(event);
            for (EventRegistration reg : regs) {
                if (!reg.isAttendanceMarked()) {
                    reg.setAttendanceMarked(true);
                    reg.setAttendedAt(LocalDateTime.now());
                }
            }
            eventRegistrationRepository.saveAll(regs);
        }
        return "redirect:/events/admin/" + id + "/attendance?markedAll=true";
    }

    // ─────────────────────────────────────────────────────────
    //  STUDENT: Join Online Event (auto-marks attendance)
    // ─────────────────────────────────────────────────────────
    @PostMapping("/{id}/join")
    public String joinOnlineEvent(@PathVariable Long id, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/login";

        Event event = eventRepository.findById(id).orElse(null);
        if (event == null) return "redirect:/events";

        EventRegistration reg = eventRegistrationRepository.findByEventAndUser(event, user).orElse(null);
        if (reg != null && !reg.isAttendanceMarked()) {
            reg.setAttendanceMarked(true);
            reg.setAttendedAt(LocalDateTime.now());
            eventRegistrationRepository.save(reg);
        }

        // Redirect to the meeting link
        String link = event.getMeetingLink();
        if (link != null && !link.isBlank()) {
            return "redirect:" + link;
        }
        return "redirect:/events/" + id + "?joined=true";
    }

    // ─────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────
    private String completeRegistration(Event event, User user, String paymentStatus,
                                         String fullName, String email, String phone,
                                         String college, String yearOfStudy) {
        EventRegistration reg = new EventRegistration();
        reg.setEvent(event);
        reg.setUser(user);
        reg.setPaymentStatus(paymentStatus);
        reg.setRegistrationStatus("REGISTERED");
        reg.setTicketId(generateTicketId(event));
        reg.setFullName(fullName);
        reg.setEmail(email);
        reg.setPhone(phone);
        reg.setCollege(college);
        reg.setYearOfStudy(yearOfStudy);
        eventRegistrationRepository.save(reg);
        return "redirect:/events/ticket/" + reg.getTicketId();
    }

    private String generateTicketId(Event event) {
        String prefix = "ZTX";
        String fullCat = event.getCategory() != null ? event.getCategory().toUpperCase() : "EV";
        String cat = fullCat.length() >= 2 ? fullCat.substring(0, 2) : (fullCat + "X").substring(0, 2);
        String uid = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return prefix + "-" + cat + "-" + uid;
    }



    private boolean isAdmin(HttpSession session) {
        return "admin".equals(session.getAttribute("user"));
    }

    private String getDefaultImage(String category) {
        return switch (category != null ? category : "") {
            case "Gaming"   -> "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=800&auto=format&fit=crop";
            case "Talent"   -> "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=800&auto=format&fit=crop";
            case "Tech"     -> "https://images.unsplash.com/photo-1518770660439-4636190af475?w=800&auto=format&fit=crop";
            case "Sports"   -> "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=800&auto=format&fit=crop";
            case "Cultural" -> "https://images.unsplash.com/photo-1533174072545-7a4b6ad7a6c3?w=800&auto=format&fit=crop";
            default         -> "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=800&auto=format&fit=crop";
        };
    }
}
