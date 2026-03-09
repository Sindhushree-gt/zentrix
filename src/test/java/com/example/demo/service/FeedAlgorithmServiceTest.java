package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.UserActivityRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FeedAlgorithmServiceTest {

    // ── Weights mirrored from the service (for assertions) ──────────────────
    private static final double W_LIKE = 3.0;
    private static final double W_COMMENT = 5.0;
    private static final double W_SHARE = 8.0;
    private static final double W_SAVE = 10.0;
    private static final double W_RECENCY = 100.0;
    private static final double W_RELATION = 20.0;

    @Mock
    private PostRepository postRepository;
    @Mock
    private UserActivityRepository activityRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FeedAlgorithmService feedService;

    // Shared fixtures
    private User viewer;
    private User author1;
    private User author2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        viewer = makeUser(1L, "viewer");
        author1 = makeUser(2L, "author1");
        author2 = makeUser(3L, "author2");

        // By default viewer follows nobody
        viewer.setFollowing(new HashSet<>());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Personalized Feed – empty following → pulls global posts
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void testPersonalizedFeed_emptyFollowing_returnsGlobalPosts() {
        Post p = makePost(10L, author1, 0, 0, LocalDateTime.now());
        Page<Post> page = new PageImpl<>(List.of(p));

        when(userRepository.findById(viewer.getId())).thenReturn(Optional.of(viewer));
        when(postRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(page);
        stubActivityRepo(p.getId());
        when(activityRepository.findByUserIdAndTimestampAfter(eq(viewer.getId()), any())).thenReturn(List.of());

        List<Post> result = feedService.getPersonalizedFeed(viewer.getId(), 0, 10);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(postRepository).findAllByOrderByCreatedAtDesc(any(Pageable.class));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Personalized Feed – with following → only followee + own posts fetched
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void testPersonalizedFeed_withFollowing_queriesFollowingPool() {
        viewer.setFollowing(Set.of(author1));

        Post p = makePost(20L, author1, 2, 1, LocalDateTime.now());
        Page<Post> page = new PageImpl<>(List.of(p));

        when(userRepository.findById(viewer.getId())).thenReturn(Optional.of(viewer));
        when(postRepository.findByUserIdInOrderByCreatedAtDesc(anyList(), any(Pageable.class))).thenReturn(page);
        stubActivityRepo(p.getId());
        when(activityRepository.findByUserIdAndTimestampAfter(eq(viewer.getId()), any())).thenReturn(List.of());

        List<Post> result = feedService.getPersonalizedFeed(viewer.getId(), 0, 10);

        assertNotNull(result);
        verify(postRepository).findByUserIdInOrderByCreatedAtDesc(anyList(), any(Pageable.class));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Higher-engagement post ranks first
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void testPersonalizedFeed_higherEngagementPostRankedFirst() {
        LocalDateTime now = LocalDateTime.now();
        Post lowEngage = makePost(30L, author1, 1, 0, now); // 3 pts engagement
        Post highEngage = makePost(31L, author2, 10, 5, now); // 55 pts engagement

        Page<Post> page = new PageImpl<>(List.of(lowEngage, highEngage));

        when(userRepository.findById(viewer.getId())).thenReturn(Optional.of(viewer));
        when(postRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(page);

        // Stub engagement repos for each post
        stubActivityRepo(lowEngage.getId());
        stubActivityRepo(highEngage.getId());
        when(activityRepository.findByUserIdAndTimestampAfter(eq(viewer.getId()), any())).thenReturn(List.of());

        List<Post> result = feedService.getPersonalizedFeed(viewer.getId(), 0, 10);

        assertEquals(highEngage.getId(), result.get(0).getId(),
                "Higher-engagement post should be ranked first");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. Newer post outscores older post with equal engagement
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void testPersonalizedFeed_recentPostRankedAboveOlderPost() {
        Post newPost = makePost(40L, author1, 0, 0, LocalDateTime.now()); // recency ≈ 100
        Post oldPost = makePost(41L, author2, 0, 0, LocalDateTime.now().minusHours(72)); // recency ≈ 1.4

        Page<Post> page = new PageImpl<>(List.of(oldPost, newPost)); // old first, should be re-ranked

        when(userRepository.findById(viewer.getId())).thenReturn(Optional.of(viewer));
        when(postRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(page);
        stubActivityRepo(newPost.getId());
        stubActivityRepo(oldPost.getId());
        when(activityRepository.findByUserIdAndTimestampAfter(eq(viewer.getId()), any())).thenReturn(List.of());

        List<Post> result = feedService.getPersonalizedFeed(viewer.getId(), 0, 10);

        assertEquals(newPost.getId(), result.get(0).getId(),
                "Newer post should outrank older post with equal engagement");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. Relationship bonus: followed author's post ranks above stranger's post
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void testPersonalizedFeed_relationshipBonusApplied() {
        viewer.setFollowing(Set.of(author1)); // viewer follows author1

        LocalDateTime now = LocalDateTime.now();
        Post followedPost = makePost(50L, author1, 0, 0, now); // +W_RELATION bonus
        Post strangerPost = makePost(51L, author2, 0, 0, now); // no bonus

        Page<Post> page = new PageImpl<>(List.of(strangerPost, followedPost));

        when(userRepository.findById(viewer.getId())).thenReturn(Optional.of(viewer));
        when(postRepository.findByUserIdInOrderByCreatedAtDesc(anyList(), any(Pageable.class))).thenReturn(page);
        stubActivityRepo(followedPost.getId());
        stubActivityRepo(strangerPost.getId());
        when(activityRepository.findByUserIdAndTimestampAfter(eq(viewer.getId()), any())).thenReturn(List.of());

        List<Post> result = feedService.getPersonalizedFeed(viewer.getId(), 0, 10);

        assertEquals(followedPost.getId(), result.get(0).getId(),
                "Followed author's post should rank first due to relationship bonus");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. Trending – sorted by engagement descending
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void testTrendingPosts_returnsSortedByEngagement() {
        LocalDateTime now = LocalDateTime.now();
        Post low = makePost(60L, author1, 1, 0, now);
        Post high = makePost(61L, author2, 20, 10, now);

        when(postRepository.findByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(List.of(low, high));
        stubActivityRepo(low.getId());
        stubActivityRepo(high.getId());

        List<Post> result = feedService.getTrendingPosts(10);

        assertEquals(high.getId(), result.get(0).getId(),
                "Trending: highest engagement post should be first");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. Trending – respects limit
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void testTrendingPosts_respectsLimit() {
        LocalDateTime now = LocalDateTime.now();
        List<Post> posts = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Post p = makePost((long) (70 + i), author1, i, i, now.minusMinutes(i));
            posts.add(p);
            stubActivityRepo(p.getId());
        }

        when(postRepository.findByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(posts);

        List<Post> result = feedService.getTrendingPosts(5);

        assertTrue(result.size() <= 5, "Trending should respect the requested limit");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 8. Recommendations – excludes followed users and self
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void testRecommendedPosts_excludesFollowedAndSelf() {
        viewer.setFollowing(Set.of(author1));

        // author3 is a stranger
        User author3 = makeUser(4L, "author3");
        Post strangerPost = makePost(80L, author3, 5, 2, LocalDateTime.now());
        Page<Post> page = new PageImpl<>(List.of(strangerPost));

        when(userRepository.findById(viewer.getId())).thenReturn(Optional.of(viewer));
        when(postRepository.findByUserIdNotInOrderByCreatedAtDesc(anyList(), any(Pageable.class))).thenReturn(page);
        stubActivityRepo(strangerPost.getId());

        List<Post> result = feedService.getRecommendedPosts(viewer.getId(), 10);

        // Should not contain author1 or viewer posts
        result.forEach(p -> {
            assertNotEquals(viewer.getId(), p.getUser().getId(), "Should not recommend own posts");
            assertNotEquals(author1.getId(), p.getUser().getId(), "Should not recommend posts from followed user");
        });
        verify(postRepository).findByUserIdNotInOrderByCreatedAtDesc(anyList(), any(Pageable.class));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private User makeUser(Long id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setFollowing(new HashSet<>());
        return u;
    }

    private Post makePost(Long id, User author, int likes, int comments, LocalDateTime createdAt) {
        Post p = new Post();
        p.setId(id);
        p.setUser(author);
        p.setCreatedAt(createdAt);
        p.setContent("Test post " + id);

        // Add fake likes
        List<PostLike> likeList = new ArrayList<>();
        for (int i = 0; i < likes; i++) {
            PostLike pl = new PostLike();
            likeList.add(pl);
        }
        p.setLikes(likeList);

        // Add fake comments
        List<PostComment> commentList = new ArrayList<>();
        for (int i = 0; i < comments; i++) {
            PostComment pc = new PostComment();
            commentList.add(pc);
        }
        p.setComments(commentList);

        return p;
    }

    /** Stub the three aggregate queries in calcEngagement() to return 0 */
    private void stubActivityRepo(Long postId) {
        when(activityRepository.countByPostIdAndActivityType(eq(postId), eq(ActivityType.SHARE))).thenReturn(0L);
        when(activityRepository.countByPostIdAndActivityType(eq(postId), eq(ActivityType.SAVE))).thenReturn(0L);
        when(activityRepository.sumWatchTimeByPostId(eq(postId))).thenReturn(0L);
    }
}
