package com.example.demo.snake;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.example.demo.chess.ChatMessage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class SnakeWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    // In-memory room store (replace with Redis/DB for production)
    private final Map<String, SnakeRoom> rooms = new ConcurrentHashMap<>();

    public SnakeWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // REST: Create Room
    @PostMapping("/api/snake/create")
    @ResponseBody
    public Map<String, String> createRoom(@RequestBody Map<String, String> body) {
        String roomId = generateRoomId();
        SnakeRoom room = new SnakeRoom(roomId, body.get("playerName"));
        rooms.put(roomId, room);
        return Map.of("roomId", roomId, "playerNum", "1");
    }

    // REST: Join Room
    @PostMapping("/api/snake/join")
    @ResponseBody
    public Map<String, String> joinRoom(@RequestBody Map<String, String> body) {
        String roomId = body.get("roomId").toUpperCase();
        String playerName = body.get("playerName");

        SnakeRoom room = rooms.get(roomId);
        if (room == null)
            return Map.of("error", "Room not found");
        if (room.player2 != null)
            return Map.of("error", "Room is full");

        room.player2 = playerName;
        room.status = "active";

        // Notify the P1 that opponent joined
        messagingTemplate.convertAndSend("/topic/snake/" + roomId, (Object) room.toStateMap());
        return Map.of("roomId", roomId, "playerNum", "2");
    }

    // WebSocket: Roll Dice (Sends roll value, then updates state)
    @MessageMapping("/snake/{roomId}/roll")
    public void rollDice(@DestinationVariable String roomId, Map<String, Integer> payload) {
        SnakeRoom room = rooms.get(roomId);
        if (room == null)
            return;

        int steps = payload.get("steps");
        int playerNum = payload.get("playerNum");

        // Broadcast the roll animation first so clients know what was rolled
        messagingTemplate.convertAndSend("/topic/snake/" + roomId + "/rollEvent", (Object) payload);

        // Update Backend state
        room.applyRoll(steps, playerNum);

        // Broadcast new final positions
        messagingTemplate.convertAndSend("/topic/snake/" + roomId, (Object) room.toStateMap());
    }

    // WebSocket: Chat
    @MessageMapping("/snake/{roomId}/chat")
    public void chat(@DestinationVariable String roomId, ChatMessage msg) {
        messagingTemplate.convertAndSend("/topic/snake/" + roomId + "/chat", (Object) msg);
    }

    // WebSocket: Subscribe to room state
    @MessageMapping("/snake/{roomId}/subscribe")
    @SendTo("/topic/snake/{roomId}")
    public Map<String, Object> subscribe(@DestinationVariable String roomId) {
        SnakeRoom room = rooms.get(roomId);
        return room != null ? room.toStateMap() : Map.of("error", "Room not found");
    }

    private String generateRoomId() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(6);
        Random rnd = new Random();
        for (int i = 0; i < 6; i++)
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }
}
