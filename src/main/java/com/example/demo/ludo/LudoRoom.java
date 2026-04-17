package com.example.demo.ludo;
import java.util.*;

public class LudoRoom {
    public String id;
    public List<String> players = new ArrayList<>();
    public int turn = 0; // index in players
    public int currentDice = 1;
    public boolean diceRolled = false;
    public String status = "waiting";
    // Simplified piece state: color -> piece index -> position (0 to 57)
    public Map<Integer, int[]> pieces = new HashMap<>(); // playerIndex -> [pos1, pos2, pos3, pos4]

    public LudoRoom(String id, String hostName) {
        this.id = id;
        players.add(hostName);
        pieces.put(0, new int[]{0, 0, 0, 0});
    }

    public synchronized void join(String name) {
        if (players.size() < 4) {
            players.add(name);
            pieces.put(players.size() - 1, new int[]{0, 0, 0, 0});
            if (players.size() >= 2) status = "active";
        }
    }

    public synchronized void rollDice(int playerIdx) {
        if (turn != playerIdx || diceRolled) return;
        currentDice = (int)(Math.random() * 6) + 1;
        diceRolled = true;
    }

    public synchronized void movePiece(int playerIdx, int pieceIdx) {
        if (turn != playerIdx || !diceRolled) return;
        int[] p = pieces.get(playerIdx);
        p[pieceIdx] += currentDice;
        
        diceRolled = false;
        turn = (turn + 1) % players.size();
    }

    public Map<String, Object> toStateMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("players", players);
        map.put("turn", turn);
        map.put("currentDice", currentDice);
        map.put("diceRolled", diceRolled);
        map.put("status", status);
        map.put("pieces", pieces);
        return map;
    }
}
