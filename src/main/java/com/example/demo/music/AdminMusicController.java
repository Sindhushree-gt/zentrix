package com.example.demo.music;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Controller
public class AdminMusicController {

    @Autowired
    private MusicService musicService;

    @Autowired
    private TrackRepository trackRepository;

    @GetMapping("/admin/music")
    public String adminMusic(Model model, HttpSession session) {
        if (!"admin".equals(session.getAttribute("user"))) return "redirect:/login";

        List<Track> pending = musicService.listPending();
        model.addAttribute("pendingTracks", pending);
        return "admin-music";
    }

    @GetMapping("/admin/music/preview/{trackId}")
    public ResponseEntity<Resource> preview(@PathVariable Long trackId, HttpSession session) {
        if (!"admin".equals(session.getAttribute("user"))) throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED);

        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));

        try {
            Path p = Path.of(track.getStoragePath());
            if (!Files.exists(p)) throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);

            Resource res = new UrlResource(p.toUri());
            MediaType mt;
            try {
                mt = track.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(track.getContentType());
            } catch (Exception e) {
                mt = MediaType.APPLICATION_OCTET_STREAM;
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"track-" + track.getId() + "\"")
                    .contentType(mt)
                    .contentLength(res.contentLength())
                    .body(res);
        } catch (MalformedURLException e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/admin/music/{trackId}/approve")
    public String approve(@PathVariable Long trackId, HttpSession session) {
        if (!"admin".equals(session.getAttribute("user"))) return "redirect:/login";

        Track t = trackRepository.findById(trackId).orElse(null);
        if (t != null) {
            t.setStatus(TrackStatus.APPROVED);
            t.setApprovedAt(LocalDateTime.now());
            trackRepository.save(t);
        }
        return "redirect:/admin/music";
    }

    @PostMapping("/admin/music/{trackId}/reject")
    public String reject(@PathVariable Long trackId, HttpSession session) {
        if (!"admin".equals(session.getAttribute("user"))) return "redirect:/login";

        Track t = trackRepository.findById(trackId).orElse(null);
        if (t != null) {
            t.setStatus(TrackStatus.REJECTED);
            trackRepository.save(t);
        }
        return "redirect:/admin/music";
    }
}

