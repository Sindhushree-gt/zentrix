package com.example.demo.uno;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class UnoWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, UnoRoom> rooms = new ConcurrentHashMap<>();

    public UnoWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/api/uno/create")
    @ResponseBody
    public Map<String, Object> createRoom(@RequestBody Map<String, String> body) {
        String roomId = generateRoomId();
        UnoRoom room = new UnoRoom(roomId, body.get("playerName"));
        rooms.put(roomId, room);
        return Map.of("roomId", roomId, "playerNum", 1);
    }

    @PostMapping("/api/uno/join")
    @ResponseBody
    public Map<String, Object> joinRoom(@RequestBody Map<String, String> body) {
        String roomId = body.get("roomId").toUpperCase();
        String playerName = body.get("playerName");
        UnoRoom room = rooms.get(roomId);
        if (room == null || room.p2Name != null) return Map.of("error", "Room invalid");
        room.p2Name = playerName;
        room.start();
        broadcastState(roomId);
        return Map.of("roomId", roomId, "playerNum", 2);
    }

    @MessageMapping("/uno/{roomId}/play")
    public void play(@DestinationVariable String roomId, Map<String, Object> msg) {
        UnoRoom room = rooms.get(roomId);
        if (room == null) return;
        room.playCard(((Number)msg.get("player")).intValue(), ((Number)msg.get("cardId")).intValue(), (String)msg.get("color"));
        broadcastState(roomId);
    }

    @MessageMapping("/uno/{roomId}/draw")
    public void draw(@DestinationVariable String roomId, Map<String, Object> msg) {
        UnoRoom room = rooms.get(roomId);
        if (room == null) return;
        room.drawCard(((Number)msg.get("player")).intValue());
        broadcastState(roomId);
    }

    @MessageMapping("/uno/{roomId}/subscribe")
    public void subscribe(@DestinationVariable String roomId) {
        broadcastState(roomId);
    }

    private void broadcastState(String roomId) {
        UnoRoom room = rooms.get(roomId);
        if (room == null) return;
        messagingTemplate.convertAndSend("/topic/uno/" + roomId + "/1", room.toStateMap(1));
        messagingTemplate.convertAndSend("/topic/uno/" + roomId + "/2", room.toStateMap(2));
    }

    private String generateRoomId() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
