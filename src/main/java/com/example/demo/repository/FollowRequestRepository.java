package com.example.demo.repository;

import com.example.demo.model.FollowRequest;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FollowRequestRepository extends JpaRepository<FollowRequest, Long> {
    Optional<FollowRequest> findBySenderAndReceiver(User sender, User receiver);

    void deleteBySenderAndReceiver(User sender, User receiver);
}
