package com.example.demo.snake;

import java.util.HashMap;
import java.util.Map;

import com.example.demo.chess.ChatMessage;

public class SnakeRoom {
    public String roomId;
    public String player1; // Creator (Red)
    public String player2; // Joiner (Blue)

    public int p1Position = 0;
    public int p2Position = 0;
    public int turn = 1; // 1 = player1, 2 = player2
    public String status = "waiting"; // waiting, active, finished

    // Immutable board data
    private static final Map<Integer, Integer> MAPS = new HashMap<>();
    static {
        // Ladders
        MAPS.put(2, 38);
        MAPS.put(7, 14);
        MAPS.put(8, 31);
        MAPS.put(15, 26);
        MAPS.put(21, 42);
        MAPS.put(28, 84);
        MAPS.put(36, 44);
        MAPS.put(51, 67);
        MAPS.put(71, 91);
        MAPS.put(78, 98);
        // Snakes
        MAPS.put(16, 6);
        MAPS.put(46, 25);
        MAPS.put(49, 11);
        MAPS.put(62, 19);
        MAPS.put(64, 60);
        MAPS.put(74, 53);
        MAPS.put(89, 68);
        MAPS.put(92, 88);
        MAPS.put(95, 75);
        MAPS.put(99, 80);
    }

    public SnakeRoom(String roomId, String player1) {
        this.roomId = roomId;
        this.player1 = player1;
    }

    public void applyRoll(int steps, int requestingPlayerNum) {
        if (!status.equals("active") || turn != requestingPlayerNum) {
            return;
        }

        int targetPos = (turn == 1) ? p1Position + steps : p2Position + steps;

        if (targetPos <= 100) {
            if (MAPS.containsKey(targetPos)) {
                targetPos = MAPS.get(targetPos);
            }

            if (turn == 1) {
                p1Position = targetPos;
            } else {
                p2Position = targetPos;
            }

            if (targetPos == 100) {
                status = "finished";
                return; // Game over, no turn swap
            }
        }

        // Swap turn
        turn = (turn == 1) ? 2 : 1;
    }

    public Map<String, Object> toStateMap() {
        Map<String, Object> state = new HashMap<>();
        state.put("roomId", roomId);
        state.put("status", status);
        state.put("turn", turn);
        state.put("p1Position", p1Position);
        state.put("p2Position", p2Position);

        Map<String, String> players = new HashMap<>();
        if (player1 != null)
            players.put("1", player1);
        if (player2 != null)
            players.put("2", player2);
        state.put("players", players);

        return state;
    }
}
