package com.example.demo.music.room;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class MusicRoomWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MusicRoomRepository roomRepository;

    public MusicRoomWebSocketController(SimpMessagingTemplate messagingTemplate, MusicRoomRepository roomRepository) {
        this.messagingTemplate = messagingTemplate;
        this.roomRepository = roomRepository;
    }

    public void broadcastRoomEvent(String code, Map<String, Object> payload) {
        messagingTemplate.convertAndSend("/topic/music-room/" + code, MessageBuilder.withPayload(payload).build());
    }

    @MessageMapping("/music-room/{code}/subscribe")
    @SendTo("/topic/music-room/{code}")
    public Map<String, Object> subscribe(@DestinationVariable String code) {
        boolean exists = roomRepository.findByCode(code.trim().toUpperCase()).isPresent();
        return exists ? Map.of("type", "hello") : Map.of("type", "error", "message", "Room not found");
    }
}

