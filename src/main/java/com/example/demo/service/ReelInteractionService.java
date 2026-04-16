package com.example.demo.service;

import com.example.demo.model.Reel;
import com.example.demo.model.User;
import com.example.demo.model.UserInterestProfile;
import com.example.demo.model.UserReelInteraction;
import com.example.demo.repository.ReelRepository;
import com.example.demo.repository.UserInterestProfileRepository;
import com.example.demo.repository.UserReelInteractionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReelInteractionService {

    @Autowired
    private UserReelInteractionRepository interactionRepository;

    @Autowired
    private UserInterestProfileRepository profileRepository;

    @Autowired
    private ReelRepository reelRepository;

    // Weights for how much each action impacts the category score
    private static final double SCORE_LIKE = 2.0;
    private static final double SCORE_COMMENT = 3.0;
    private static final double SCORE_SHARE = 5.0;
    private static final double SCORE_SAVE = 4.0;
    private static final double SCORE_COMPLETED = 1.0;
    private static final double SCORE_WATCH_SECOND = 0.1;

    @Transactional
    public void recordInteraction(User user, Long reelId, String actionType, Integer watchTimeVal) {
        Reel reel = reelRepository.findById(reelId)
                .orElseThrow(() -> new RuntimeException("Reel not found"));

        UserReelInteraction interaction = interactionRepository.findByUserIdAndReelId(user.getId(), reelId)
                .orElse(new UserReelInteraction(user, reel));

        UserInterestProfile profile = profileRepository.findByUserId(user.getId())
                .orElse(new UserInterestProfile(user));

        boolean newlyLiked = false, newlyCommented = false, newlyShared = false, newlySaved = false,
                newlyCompleted = false;

        switch (actionType.toLowerCase()) {
            case "like":
                if (!interaction.isLiked()) {
                    interaction.setLiked(true);
                    newlyLiked = true;
                    reel.incrementLikeCount();
                }
                break;
            case "unlike":
                if (interaction.isLiked()) {
                    interaction.setLiked(false);
                    profile.incrementScore(reel.getCategory(), -SCORE_LIKE); // penalty
                    reel.decrementLikeCount();
                }
                break;
            case "comment":
                interaction.setCommented(true);
                newlyCommented = true;
                reel.incrementCommentCount();
                break;
            case "share":
                interaction.setShared(true);
                newlyShared = true;
                reel.incrementShareCount();
                break;
            case "save":
                interaction.setSaved(true);
                newlySaved = true;
                reel.incrementSaveCount();
                break;
            case "complete":
                if (!interaction.isCompleted()) {
                    interaction.setCompleted(true);
                    newlyCompleted = true;
                }
                break;
            case "watch":
                if (watchTimeVal != null && watchTimeVal > interaction.getWatchTime()) {
                    // incremental watch time
                    long addedTime = watchTimeVal - interaction.getWatchTime();
                    interaction.setWatchTime(watchTimeVal);
                    profile.incrementScore(reel.getCategory(), addedTime * SCORE_WATCH_SECOND);

                    if (interaction.getWatchTime() < 3) {
                        interaction.setSkipped(true);
                    } else {
                        interaction.setSkipped(false);
                        reel.incrementViewCount(); // Count view if watched more than 3s
                    }
                }
                break;
        }

        // Apply profile impacts
        if (newlyLiked)
            profile.incrementScore(reel.getCategory(), SCORE_LIKE);
        if (newlyCommented)
            profile.incrementScore(reel.getCategory(), SCORE_COMMENT);
        if (newlyShared)
            profile.incrementScore(reel.getCategory(), SCORE_SHARE);
        if (newlySaved)
            profile.incrementScore(reel.getCategory(), SCORE_SAVE);
        if (newlyCompleted)
            profile.incrementScore(reel.getCategory(), SCORE_COMPLETED);

        interactionRepository.save(interaction);
        profileRepository.save(profile);
        reelRepository.save(reel);
    }
}
