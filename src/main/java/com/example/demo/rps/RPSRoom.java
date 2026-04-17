package com.example.demo.rps;
import java.util.*;

public class RPSRoom {
    public String id;
    public String p1Name;
    public String p2Name;
    public String p1Choice = "";
    public String p2Choice = "";
    public int p1Score = 0;
    public int p2Score = 0;
    public String status = "waiting"; // waiting, active, result
    public String lastResult = "";

    public RPSRoom(String id, String p1Name) {
        this.id = id;
        this.p1Name = p1Name;
    }

    public synchronized void makeChoice(int playerNum, String choice) {
        if (playerNum == 1) p1Choice = choice;
        else p2Choice = choice;

        if (!p1Choice.isEmpty() && !p2Choice.isEmpty()) {
            calculateWinner();
            status = "result";
        }
    }

    private void calculateWinner() {
        if (p1Choice.equals(p2Choice)) {
            lastResult = "draw";
        } else if (
            (p1Choice.equals("Rock") && p2Choice.equals("Scissors")) ||
            (p1Choice.equals("Paper") && p2Choice.equals("Rock")) ||
            (p1Choice.equals("Scissors") && p2Choice.equals("Paper"))
        ) {
            lastResult = "p1";
            p1Score++;
        } else {
            lastResult = "p2";
            p2Score++;
        }
    }

    public void nextRound() {
        p1Choice = "";
        p2Choice = "";
        status = "active";
        lastResult = "";
    }

    public Map<String, Object> toStateMap(int forPlayer) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("p1Name", p1Name);
        map.put("p2Name", p2Name);
        
        // Obfuscate choices unless in result state
        if (status.equals("result")) {
            map.put("p1Choice", p1Choice);
            map.put("p2Choice", p2Choice);
        } else {
            map.put("p1Choice", !p1Choice.isEmpty() ? "locked" : "");
            map.put("p2Choice", !p2Choice.isEmpty() ? "locked" : "");
            // Show player their own choice
            if (forPlayer == 1) map.put("myChoice", p1Choice);
            if (forPlayer == 2) map.put("myChoice", p2Choice);
        }
        
        map.put("p1Score", p1Score);
        map.put("p2Score", p2Score);
        map.put("status", status);
        map.put("lastResult", lastResult);
        return map;
    }
}
