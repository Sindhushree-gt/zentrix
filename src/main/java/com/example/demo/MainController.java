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

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/admin")
    public String admin() {
        return "admin-dashboard";
    }
}
