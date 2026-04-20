package com.example.demo.service;

import com.example.demo.model.RewardConfig;
import com.example.demo.model.User;
import com.example.demo.repository.RewardConfigRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class RewardService {

    @Autowired
    private RewardConfigRepository rewardConfigRepository;

    @Autowired
    private UserRepository userRepository;

    public RewardConfig getConfig() {
        return rewardConfigRepository.findAll().stream().findFirst().orElseGet(() -> {
            RewardConfig newConfig = new RewardConfig();
            return rewardConfigRepository.save(newConfig);
        });
    }

    public void awardDailyLogin(User user) {
        if (user.getLastLoginDate() == null || !user.getLastLoginDate().equals(LocalDate.now())) {
            RewardConfig config = getConfig();
            user.addCoins(config.getDailyLogin());
            user.setLastLoginDate(LocalDate.now());
            userRepository.save(user);
        }
    }

    public void awardVoting(User user) {
        RewardConfig config = getConfig();
        user.addCoins(config.getVoteInEvent());
        userRepository.save(user);
    }

    public void awardMusicVote(User user) {
        RewardConfig config = getConfig();
        user.addCoins(config.getMusicVote());
        userRepository.save(user);
    }

    public void awardRegistration(User user) {
        RewardConfig config = getConfig();
        user.addCoins(config.getRegisterForEvent());
        userRepository.save(user);
    }

    public void awardAttendance(User user) {
        RewardConfig config = getConfig();
        user.addCoins(config.getAttendEvent());
        userRepository.save(user);
    }

    public void awardWinner(User user) {
        RewardConfig config = getConfig();
        user.addCoins(config.getWinner());
        userRepository.save(user);
    }

    public void awardRunner(User user) {
        RewardConfig config = getConfig();
        user.addCoins(config.getRunner());
        userRepository.save(user);
    }

    public void awardReferral(User user) {
        RewardConfig config = getConfig();
        user.addCoins(config.getReferFriend());
        userRepository.save(user);
    }

    public void awardTalentPost(User user) {
        RewardConfig config = getConfig();
        user.addCoins(config.getTalentPost());
        userRepository.save(user);
    }

    /**
     * MVP: reward coins for listening at least N seconds in a day.
     * Uses user fields to avoid extending RewardConfig/admin form.
     */
    public boolean awardDailyMusicListening(User user, int newlyRewardableSeconds) {
        if (user == null) return false;

        LocalDate today = LocalDate.now();
        if (user.getLastMusicRewardDate() != null && user.getLastMusicRewardDate().equals(today)) {
            return false;
        }

        // reset counter if it belongs to a previous day
        if (user.getMusicSecondsDate() == null || !user.getMusicSecondsDate().equals(today)) {
            user.setMusicRewardedSecondsToday(0);
            user.setMusicSecondsDate(today);
        }
        if (user.getMusicSecondsDate() == null) {
            user.setMusicRewardedSecondsToday(0);
        }

        int updatedSeconds = Math.min(60 * 60, user.getMusicRewardedSecondsToday() + Math.max(0, newlyRewardableSeconds)); // cap 1h/day
        user.setMusicRewardedSecondsToday(updatedSeconds);

        int thresholdSeconds = 10 * 60; // 10 minutes
        if (updatedSeconds >= thresholdSeconds) {
            user.addCoins(3); // keep small for MVP
            user.setLastMusicRewardDate(today);
        }

        userRepository.save(user);
        return user.getLastMusicRewardDate() != null && user.getLastMusicRewardDate().equals(today);
    }
}
