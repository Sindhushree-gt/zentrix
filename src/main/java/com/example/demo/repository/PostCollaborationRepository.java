package com.example.demo.repository;

import com.example.demo.model.CollaborationStatus;
import com.example.demo.model.Post;
import com.example.demo.model.PostCollaboration;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostCollaborationRepository extends JpaRepository<PostCollaboration, Long> {
    List<PostCollaboration> findByUserAndStatus(User user, CollaborationStatus status);

    List<PostCollaboration> findByPostAndStatus(Post post, CollaborationStatus status);

    List<PostCollaboration> findByUserAndStatusOrderByPostCreatedAtDesc(User user, CollaborationStatus status);
}
