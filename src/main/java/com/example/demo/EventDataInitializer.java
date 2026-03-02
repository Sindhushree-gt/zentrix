package com.example.demo;

import com.example.demo.model.Event;
import com.example.demo.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class EventDataInitializer implements CommandLineRunner {

    @Autowired
    private EventRepository eventRepository;

    @Override
    public void run(String... args) throws Exception {
        if (eventRepository.count() == 0) {
            // Sports
            createEvent("Summer Cricket League",
                    "The biggest inter-college cricket tournament of the year. 16 teams, 1 trophy. Witness the adrenaline!",
                    "https://images.unsplash.com/photo-1531415074968-036ba1b575da?q=80&w=2070&auto=format&fit=crop",
                    LocalDateTime.now().plusDays(10).withHour(10).withMinute(0),
                    "Main Cricket Ground", "Free", "Sports", "Zentrix Sports Club");

            createEvent("Zentrix Football Cup",
                    "A 5-a-side football tournament for all skill levels. Bring your squad and dominate the pitch!",
                    "https://images.unsplash.com/photo-1574629810360-7efbbe195018?q=80&w=1986&auto=format&fit=crop",
                    LocalDateTime.now().plusDays(15).withHour(16).withMinute(0),
                    "Blue Turf Arena", "$15 Team", "Sports", "FC Zentrix");

            // Cultural
            createEvent("Rhythmic Night Fest",
                    "An evening of music, dance, and artistic expression. Featuring local bands and cultural performances.",
                    "https://images.unsplash.com/photo-1540039155733-5bb30b53aa14?q=80&w=1974&auto=format&fit=crop",
                    LocalDateTime.now().plusDays(20).withHour(18).withMinute(30),
                    "Open Air Amphitheatre", "Free", "Cultural", "Arts Council");

            createEvent("Zentrix Film Festival",
                    "Showcasing short films and documentaries from talented student creators. Awards for Best Director and Screenplay.",
                    "https://images.unsplash.com/photo-1485846234645-a62644f84728?q=80&w=2059&auto=format&fit=crop",
                    LocalDateTime.now().plusDays(25).withHour(17).withMinute(0),
                    "Auditorium B", "$5 Entry", "Cultural", "Media Society");

            // Tech
            createEvent("Tech Hackathon 2024",
                    "48 hours of intense coding, problem-solving, and innovation. Build the future with Zentrix!",
                    "https://images.unsplash.com/photo-1504384308090-c894fdcc538d?q=80&w=2070&auto=format&fit=crop",
                    LocalDateTime.now().plusDays(5).withHour(9).withMinute(0),
                    "Digital Innovation Hub", "$10 Registration", "Tech", "Tech Innovators");

            createEvent("AI & Future Workshop",
                    "Learn about the latest trends in Artificial Intelligence and Machine Learning from industry experts.",
                    "https://images.unsplash.com/photo-1591453089816-0fbb971b404c?q=80&w=2070&auto=format&fit=crop",
                    LocalDateTime.now().plusDays(12).withHour(14).withMinute(0),
                    "Seminar Hall 1", "Free", "Workshop", "AI Research Lab");

            // E-Sports
            createEvent("Valorant Ultimate Showdown",
                    "Compete against the best tactical shooters in the college. High stakes, big prizes!",
                    "https://images.unsplash.com/photo-1542751371-adc38448a05e?q=80&w=2070&auto=format&fit=crop",
                    LocalDateTime.now().plusDays(8).withHour(15).withMinute(0),
                    "Gaming Lounge / Online", "$2 Entry", "E-Sports", "Pro Gamers League");
        }
    }

    private void createEvent(String title, String desc, String url, LocalDateTime dt, String venue, String price,
            String cat, String org) {
        Event event = new Event();
        event.setTitle(title);
        event.setDescription(desc);
        event.setImageUrl(url);
        event.setDateTime(dt);
        event.setVenue(venue);
        event.setPrice(price);
        event.setCategory(cat);
        event.setOrganizer(org);
        eventRepository.save(event);
    }
}
