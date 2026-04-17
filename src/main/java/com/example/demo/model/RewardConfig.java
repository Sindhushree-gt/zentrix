package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "reward_configs")
public class RewardConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int dailyLogin = 5;
    private int voteInEvent = 2;
    private int registerForEvent = 10;
    private int attendEvent = 15;
    private int winner = 50;
    private int runner = 30;
    private int referFriend = 20;
    private int talentPost = 5;
    private int musicVote = 1;

    public RewardConfig() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getDailyLogin() { return dailyLogin; }
    public void setDailyLogin(int dailyLogin) { this.dailyLogin = dailyLogin; }

    public int getVoteInEvent() { return voteInEvent; }
    public void setVoteInEvent(int voteInEvent) { this.voteInEvent = voteInEvent; }

    public int getRegisterForEvent() { return registerForEvent; }
    public void setRegisterForEvent(int registerForEvent) { this.registerForEvent = registerForEvent; }

    public int getAttendEvent() { return attendEvent; }
    public void setAttendEvent(int attendEvent) { this.attendEvent = attendEvent; }

    public int getWinner() { return winner; }
    public void setWinner(int winner) { this.winner = winner; }

    public int getRunner() { return runner; }
    public void setRunner(int runner) { this.runner = runner; }

    public int getReferFriend() { return referFriend; }
    public void setReferFriend(int referFriend) { this.referFriend = referFriend; }

    public int getTalentPost() { return talentPost; }
    public void setTalentPost(int talentPost) { this.talentPost = talentPost; }

    public int getMusicVote() { return musicVote; }
    public void setMusicVote(int musicVote) { this.musicVote = musicVote; }
}
