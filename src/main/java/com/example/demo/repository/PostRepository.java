package com.example.demo.repository;

import com.example.demo.model.Post;
import com.example.demo.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findAllByOrderByCreatedAtDesc();

    List<Post> findByUserOrderByCreatedAtDesc(User user);

    /** Paginated feed of all posts, newest first */
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Posts by a specific set of user IDs (following) */
    Page<Post> findByUserIdInOrderByCreatedAtDesc(List<Long> userIds, Pageable pageable);

    /** Posts NOT from given user IDs (for recommendations) */
    Page<Post> findByUserIdNotInOrderByCreatedAtDesc(List<Long> excludedUserIds, Pageable pageable);

    /** Posts created after a given time — used for trending window */
    List<Post> findByCreatedAtAfter(LocalDateTime since);
}
