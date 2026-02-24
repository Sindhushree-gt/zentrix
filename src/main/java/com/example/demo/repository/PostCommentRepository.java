package com.example.demo.repository;

import com.example.demo.model.Post;
import com.example.demo.model.PostComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PostCommentRepository extends JpaRepository<PostComment, Long> {
    List<PostComment> findByPostOrderByCreatedAtAsc(Post post);

    long countByPost(Post post);
}
