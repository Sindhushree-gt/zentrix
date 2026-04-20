package com.example.demo.ludo;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class LudoWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, LudoRoom> rooms = new ConcurrentHashMap<>();

    public LudoWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/api/ludo/create")
    @ResponseBody
    public Map<String, Object> createRoom(@RequestBody Map<String, String> body) {
        String roomId = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        LudoRoom room = new LudoRoom(roomId, body.get("playerName"));
        rooms.put(roomId, room);
        return Map.of("roomId", roomId, "playerIndex", 0);
    }

    @PostMapping("/api/ludo/join")
    @ResponseBody
    public Map<String, Object> joinRoom(@RequestBody Map<String, String> body) {
        String roomId = body.get("roomId").toUpperCase();
        LudoRoom room = rooms.get(roomId);
        if (room == null || room.players.size() >= 4) return Map.of("error", "Full or not found");
        
        int idx = room.players.size();
        room.join(body.get("playerName"));
        broadcastState(roomId);
        return Map.of("roomId", roomId, "playerIndex", idx);
    }

    @MessageMapping("/ludo/{roomId}/roll")
    public void roll(@DestinationVariable String roomId, Map<String, Object> msg) {
        LudoRoom room = rooms.get(roomId);
        if (room != null) {
            room.rollDice(((Number)msg.get("playerIndex")).intValue());
            broadcastState(roomId);
        }
    }

    @MessageMapping("/ludo/{roomId}/move")
    public void move(@DestinationVariable String roomId, Map<String, Object> msg) {
        LudoRoom room = rooms.get(roomId);
        if (room != null) {
            room.movePiece(((Number)msg.get("playerIndex")).intValue(), ((Number)msg.get("pieceIndex")).intValue());
            broadcastState(roomId);
        }
    }

    @MessageMapping("/ludo/{roomId}/subscribe")
    public void subscribe(@DestinationVariable String roomId) {
        broadcastState(roomId);
    }

    private void broadcastState(String roomId) {
        LudoRoom room = rooms.get(roomId);
        if (room != null) {
            messagingTemplate.convertAndSend("/topic/ludo/" + roomId, room.toStateMap());
        }
    }
}
