package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.UserActivityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserActivityService {

    @Autowired
    private UserActivityRepository activityRepository;

    /**
     * Record a user activity. Safe to call multiple times — always inserts a fresh
     * record
     * so watch-time accumulation works correctly.
     */
    @Transactional
    public void record(User user, Post post, ActivityType type) {
        UserActivity activity = new UserActivity(user, post, type);
        activityRepository.save(activity);
    }

    /**
     * Record a VIEW with watch-time in seconds.
     */
    @Transactional
    public void recordView(User user, Post post, long watchTimeSeconds) {
        UserActivity activity = new UserActivity(user, post, ActivityType.VIEW);
        activity.setWatchTime(watchTimeSeconds);
        activityRepository.save(activity);
    }
}
