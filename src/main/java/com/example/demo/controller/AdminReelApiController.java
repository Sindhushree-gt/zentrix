package com.example.demo.controller;

import com.example.demo.model.Reel;
import com.example.demo.service.ReelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/reels")
public class AdminReelApiController {

    @Autowired
    private ReelService reelService;

    // 1. Approve Reel
    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approveReel(@PathVariable Long id) {
        try {
            Reel approved = reelService.approveReel(id);
            return ResponseEntity.ok(approved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Approval failed: " + e.getMessage());
        }
    }

    // 2. Delete/Reject Reel
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReel(@PathVariable Long id) {
        try {
            reelService.deleteReel(id);
            return ResponseEntity.ok("Reel deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Deletion failed: " + e.getMessage());
        }
    }

    // 3. Modulate Meta Data Manually
    @PostMapping("/{id}/metrics")
    public ResponseEntity<?> updateMetrics(
            @PathVariable Long id,
            @RequestBody Map<String, Long> metrics) {
        try {
            Reel updated = reelService.updateReelMetrics(
                    id,
                    metrics.get("views"),
                    metrics.get("likes"),
                    metrics.get("comments"));
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Metric update failed: " + e.getMessage());
        }
    }

    // Feature/Pin reel could be similar boolean flags added to standard updates.
}
