package com.example.demo.music;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.RewardService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class MusicController {

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MusicStorageService storageService;

    @Autowired
    private MusicService musicService;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private RewardService rewardService;

    private User getUser(HttpSession session) {
        Object authUser = httpServletRequest.getAttribute("authenticatedUser");
        if (authUser instanceof User) return (User) authUser;

        Object userIdObj = session.getAttribute("userId");
        if (userIdObj instanceof Long) {
            return userRepository.findById((Long) userIdObj).orElse(null);
        }
        return null;
    }

    @GetMapping("/music")
    public String musicHome(Model model, HttpSession session) {
        User user = getUser(session);
        if (user == null) return "redirect:/login";

        List<Track> tracks = musicService.listApproved();

        Map<Long, Long> likeCounts = new HashMap<>();
        for (Track t : tracks) {
            likeCounts.put(t.getId(), musicService.likeCount(t));
        }

        model.addAttribute("user", user);
        model.addAttribute("tracks", tracks);
        model.addAttribute("likeCounts", likeCounts);
        return "music";
    }

    @PostMapping("/music/upload")
    public String uploadTrack(
            @RequestParam("title") String title,
            @RequestParam("artistName") String artistName,
            @RequestParam("licenseName") String licenseName,
            @RequestParam(value = "licenseUrl", required = false) String licenseUrl,
            @RequestParam("acceptTerms") boolean acceptTerms,
            @RequestParam("file") MultipartFile file,
            HttpSession session
    ) throws IOException {
        User user = getUser(session);
        if (user == null) return "redirect:/login";
        if (!acceptTerms) return "redirect:/music?error=terms";

        String ct = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!ct.startsWith("audio/")) {
            return "redirect:/music?error=type";
        }

        MusicStorageService.StoredFile stored = storageService.store(file);

        Track track = new Track();
        track.setTitle(title == null ? "Untitled" : title.trim());
        track.setArtistName(artistName == null ? user.getUsername() : artistName.trim());
        track.setLicenseName(licenseName == null ? "" : licenseName.trim());
        track.setLicenseUrl(licenseUrl == null ? "" : licenseUrl.trim());
        track.setStoragePath(stored.absolutePath());
        track.setContentType(stored.contentType());
        track.setSizeBytes(stored.sizeBytes());
        track.setStatus(TrackStatus.PENDING);
        track.setUploader(user);
        track.setCreatedAt(LocalDateTime.now());
        trackRepository.save(track);

        return "redirect:/music?success=uploaded";
    }

    @PostMapping("/music/{trackId}/like")
    public String like(@PathVariable Long trackId, HttpSession session) {
        User user = getUser(session);
        if (user == null) return "redirect:/login";

        Track track = musicService.findById(trackId).orElse(null);
        if (track == null || track.getStatus() != TrackStatus.APPROVED) return "redirect:/music";

        musicService.toggleLike(track, user);
        return "redirect:/music";
    }

    @PostMapping("/api/music/{trackId}/listen")
    public ResponseEntity<?> listenPing(@PathVariable Long trackId, @RequestParam int seconds, HttpSession session) {
        User user = getUser(session);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Track track = musicService.findById(trackId).orElse(null);
        if (track == null || track.getStatus() != TrackStatus.APPROVED) return ResponseEntity.notFound().build();

        musicService.recordListening(user, track, seconds);
        boolean rewarded = rewardService.awardDailyMusicListening(user, Math.max(0, Math.min(seconds, 300)));

        Map<String, Object> res = new HashMap<>();
        res.put("rewarded", rewarded);
        res.put("coins", userRepository.findById(user.getId()).map(User::getCoins).orElse(0));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/music/stream/{trackId}")
    public ResponseEntity<?> stream(@PathVariable Long trackId, HttpSession session,
                                    @RequestHeader HttpHeaders headers) {
        boolean isAdmin = "admin".equals(session.getAttribute("user"));
        User user = getUser(session);
        if (!isAdmin && user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

        Track track = musicService.findById(trackId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!isAdmin && !(track.getStatus() == TrackStatus.APPROVED || track.getStatus() == TrackStatus.ROOM_ONLY)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (isAdmin && track.getStatus() == TrackStatus.TAKEDOWN) throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        Resource resource;
        try {
            Path path = Path.of(track.getStoragePath());
            if (!Files.exists(path)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            resource = new UrlResource(path.toUri());
        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        long contentLength;
        try {
            contentLength = resource.contentLength();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        MediaType mediaType;
        try {
            mediaType = track.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(track.getContentType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        HttpRange range = headers.getRange() != null && !headers.getRange().isEmpty() ? headers.getRange().get(0) : null;

        if (range != null) {
            ResourceRegion region;
            long start = range.getRangeStart(contentLength);
            long end = range.getRangeEnd(contentLength);
            long rangeLength = end - start + 1;
            region = new ResourceRegion(resource, start, rangeLength);

            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + contentLength)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .contentLength(rangeLength)
                    .body(region);
        }

        // If the browser doesn't send a Range header, serve the entire file.
        // Otherwise, playback may stop after the first chunk.
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .contentLength(contentLength)
                .body(resource);
    }
}

