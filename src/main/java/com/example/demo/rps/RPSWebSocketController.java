package com.example.demo.rps;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class RPSWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, RPSRoom> rooms = new ConcurrentHashMap<>();

    public RPSWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/api/rps/create")
    @ResponseBody
    public Map<String, Object> createRoom(@RequestBody Map<String, String> body) {
        String roomId = generateRoomId();
        RPSRoom room = new RPSRoom(roomId, body.get("playerName"));
        rooms.put(roomId, room);
        return Map.of("roomId", roomId, "playerNum", 1);
    }

    @PostMapping("/api/rps/join")
    @ResponseBody
    public Map<String, Object> joinRoom(@RequestBody Map<String, String> body) {
        String roomId = body.get("roomId").toUpperCase();
        String playerName = body.get("playerName");
        RPSRoom room = rooms.get(roomId);
        
        if (room == null) return Map.of("error", "Room not found");
        if (room.p2Name != null) return Map.of("error", "Room is full");

        room.p2Name = playerName;
        room.status = "active";
        
        broadcastState(roomId);
        return Map.of("roomId", roomId, "playerNum", 2);
    }

    @MessageMapping("/rps/{roomId}/choice")
    public void makeChoice(@DestinationVariable String roomId, Map<String, Object> msg) {
        RPSRoom room = rooms.get(roomId);
        if (room == null) return;
        
        int playerNum = (int) msg.get("playerNum");
        String choice = (String) msg.get("choice");
        room.makeChoice(playerNum, choice);
        broadcastState(roomId);
    }

    @MessageMapping("/rps/{roomId}/next")
    public void nextRound(@DestinationVariable String roomId) {
        RPSRoom room = rooms.get(roomId);
        if (room == null) return;
        room.nextRound();
        broadcastState(roomId);
    }
    
    @MessageMapping("/rps/{roomId}/subscribe")
    public void subscribe(@DestinationVariable String roomId) {
        broadcastState(roomId);
    }

    private void broadcastState(String roomId) {
        RPSRoom room = rooms.get(roomId);
        if (room == null) return;
        // In RPS, we might want to send different views to P1 and P2 
        // to hide unrevealed choices, but for simplicity here we broadcast a common state
        // with hidden fields.
        messagingTemplate.convertAndSend("/topic/rps/" + roomId, room.toStateMap(0));
    }

    private String generateRoomId() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
