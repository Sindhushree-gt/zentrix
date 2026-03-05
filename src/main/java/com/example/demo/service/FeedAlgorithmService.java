package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.UserActivityRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FeedAlgorithmService {

    // ── Scoring Weights ─────────────────────────────────────────────────────
    private static final double W_LIKE = 3.0;
    private static final double W_COMMENT = 5.0;
    private static final double W_SHARE = 8.0;
    private static final double W_SAVE = 10.0;
    private static final double W_WATCH = 2.0;
    private static final double W_RECENCY = 100.0; // max recency bonus
    private static final double W_RELATION = 20.0; // bonus for followed creators

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserActivityRepository activityRepository;

    @Autowired
    private UserRepository userRepository;

    // ── 1. Personalized Feed ─────────────────────────────────────────────────

    /**
     * Returns a personalized, score-ranked page of posts for the given user.
     * Cached for 60 s to avoid re-scoring on every request.
     */
    @Cacheable(value = "feed", key = "#userId + '-' + #page + '-' + #size")
    public List<Post> getPersonalizedFeed(Long userId, int page, int size) {
        // Fetch IDs of users the viewer follows
        Set<Long> followingIds = getFollowingIds(userId);

        // Pull a wider pool to score (e.g. last 200 posts) then trim to page
        int poolSize = Math.max(size * 10, 200);
        List<Post> pool;
        if (followingIds.isEmpty()) {
            pool = postRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, poolSize)).getContent();
        } else {
            List<Long> idList = new ArrayList<>(followingIds);
            idList.add(userId); // include own posts
            pool = postRepository.findByUserIdInOrderByCreatedAtDesc(idList, PageRequest.of(0, poolSize)).getContent();
        }

        // Collect recent activity of THIS user to compute relationship scores
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<UserActivity> viewerActivity = activityRepository.findByUserIdAndTimestampAfter(userId, since);

        // Build a set of author IDs the viewer has interacted with recently
        Set<Long> interactedAuthorIds = viewerActivity.stream()
                .map(a -> a.getPost().getUser().getId())
                .collect(Collectors.toSet());

        // Score and sort
        List<Post> ranked = pool.stream()
                .sorted(Comparator.comparingDouble(
                        (Post p) -> calcScore(p, followingIds, interactedAuthorIds)).reversed())
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());

        return ranked;
    }

    // ── 2. Trending Posts ────────────────────────────────────────────────────

    /**
     * Returns posts sorted purely by engagement score within last 48 h.
     * Cached for 5 min — trending changes slower than personal feed.
     */
    @Cacheable(value = "trending", key = "#limit")
    public List<Post> getTrendingPosts(int limit) {
        LocalDateTime window = LocalDateTime.now().minusHours(48);
        List<Post> recent = postRepository.findByCreatedAtAfter(window);

        return recent.stream()
                .sorted(Comparator.comparingDouble((Post p) -> calcEngagement(p)).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    // ── 3. Recommendations ─────────────────────────────────────────────────

    /**
     * Returns posts from users the viewer does NOT follow (discovery).
     * Cached for 2 min.
     */
    @Cacheable(value = "recommended", key = "#userId + '-' + #limit")
    public List<Post> getRecommendedPosts(Long userId, int limit) {
        Set<Long> followingIds = getFollowingIds(userId);
        List<Long> excludeIds = new ArrayList<>(followingIds);
        excludeIds.add(userId);

        Pageable pageable = PageRequest.of(0, limit * 5); // over-fetch then sort
        List<Post> candidates = postRepository
                .findByUserIdNotInOrderByCreatedAtDesc(excludeIds, pageable)
                .getContent();

        return candidates.stream()
                .sorted(Comparator.comparingDouble((Post p) -> calcEngagement(p)).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    // ── Internal Scoring ────────────────────────────────────────────────────

    private double calcScore(Post p, Set<Long> followingIds, Set<Long> interactedAuthorIds) {
        double engagement = calcEngagement(p);

        // Recency: 100 / (1 + hours_old) — gives ~100 for brand-new, decays quickly
        long hoursOld = ChronoUnit.HOURS.between(p.getCreatedAt(), LocalDateTime.now());
        double recency = W_RECENCY / (1.0 + hoursOld);

        // Relationship: bonus if viewer follows or interacted with author recently
        Long authorId = p.getUser().getId();
        double relationship = (followingIds.contains(authorId) || interactedAuthorIds.contains(authorId))
                ? W_RELATION
                : 0.0;

        double finalScore = engagement + recency + relationship;

        // Debug logging
        System.out.printf("[FeedScore] postId=%d | engagement=%.1f | recency=%.2f | relationship=%.1f | TOTAL=%.2f%n",
                p.getId(), engagement, recency, relationship, finalScore);

        return finalScore;
    }

    private double calcEngagement(Post p) {
        long likes = p.getLikes().size();
        long comments = p.getComments().size();

        // Shares and saves come from UserActivity table
        long shares = activityRepository.countByPostIdAndActivityType(p.getId(), ActivityType.SHARE);
        long saves = activityRepository.countByPostIdAndActivityType(p.getId(), ActivityType.SAVE);
        long watchTime = activityRepository.sumWatchTimeByPostId(p.getId());

        return (likes * W_LIKE)
                + (comments * W_COMMENT)
                + (shares * W_SHARE)
                + (saves * W_SAVE)
                + (watchTime * W_WATCH);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Set<Long> getFollowingIds(Long userId) {
        return userRepository.findById(userId)
                .map(u -> {
                    Set<Long> ids = new HashSet<>();
                    u.getFollowing().forEach(f -> ids.add(f.getId()));
                    return ids;
                })
                .orElse(Collections.emptySet());
    }
}
