package com.example.demo.service;

import com.example.demo.model.Reel;
import com.example.demo.model.User;
import com.example.demo.repository.ReelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class ReelService {

    @Autowired
    private ReelRepository reelRepository;

    @Value("${upload.path.reels:src/main/resources/static/uploads/reels/}")
    private String UPLOAD_DIR;

    public Reel uploadReel(User user, MultipartFile videoFile, Reel reelMetadata) throws IOException {
        // Ensure directory exists
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Save file locally
        String filename = UUID.randomUUID() + "_" + videoFile.getOriginalFilename();
        Path filePath = uploadPath.resolve(filename);
        Files.copy(videoFile.getInputStream(), filePath);

        // Prepare entity
        reelMetadata.setUser(user);
        reelMetadata.setVideoUrl("/uploads/reels/" + filename); // Public URL relative to static

        // Admins uploading get auto-approval depending on logic, else false
        if ("admin".equals(user.getUsername())) { // Or check roles/auth
            reelMetadata.setApproved(true);
        }

        return reelRepository.save(reelMetadata);
    }

    public Page<Reel> getApprovedReels(Pageable pageable) {
        return reelRepository.findByIsApprovedTrue(pageable);
    }

    public Reel getReelById(Long id) {
        return reelRepository.findById(id).orElseThrow(() -> new RuntimeException("Reel not found"));
    }

    public Reel updateReelMetrics(Long id, Long views, Long likes, Long comments) {
        Reel reel = getReelById(id);
        if (views != null)
            reel.setViewCount(views);
        if (likes != null)
            reel.setLikeCount(likes);
        if (comments != null)
            reel.setCommentCount(comments);
        return reelRepository.save(reel);
    }

    public Reel approveReel(Long id) {
        Reel reel = getReelById(id);
        reel.setApproved(true);
        return reelRepository.save(reel);
    }

    public void deleteReel(Long id) {
        reelRepository.deleteById(id);
    }
}
