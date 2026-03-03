package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChatServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ChatMessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MessageReadReceiptRepository readReceiptRepository;

    @InjectMocks
    private ChatService chatService;

    private User sender;
    private User recipient;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sender = new User();
        sender.setId(1L);
        sender.setUsername("sender");

        recipient = new User();
        recipient.setId(2L);
        recipient.setUsername("recipient");

        conversation = new Conversation();
        conversation.setId(1L);
        conversation.getParticipants().add(sender);
        conversation.getParticipants().add(recipient);
        conversation.setType(Conversation.ConversationType.DIRECT);
    }

    @Test
    void testSendMessage_NewConversation() {
        when(userRepository.findById(recipient.getId())).thenReturn(Optional.of(recipient));
        when(conversationRepository.findDirectConversationBetweenUsers(sender, recipient)).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(i -> i.getArguments()[0]);
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(i -> i.getArguments()[0]);

        ChatMessage result = chatService.sendMessage(sender, recipient.getId(), "Hello", null, null, false, false);

        assertNotNull(result);
        assertEquals("Hello", result.getContent());
        assertEquals(sender, result.getSender());
        verify(conversationRepository, times(2)).save(any(Conversation.class));
        verify(messageRepository, times(1)).save(any(ChatMessage.class));
    }

    @Test
    void testSendMessage_ExistingConversation() {
        when(userRepository.findById(recipient.getId())).thenReturn(Optional.of(recipient));
        when(conversationRepository.findDirectConversationBetweenUsers(sender, recipient))
                .thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(i -> i.getArguments()[0]);

        ChatMessage result = chatService.sendMessage(sender, recipient.getId(), "Hi again", null, null, false, false);

        assertNotNull(result);
        assertEquals("Hi again", result.getContent());
        assertEquals(conversation, result.getConversation());
        verify(messageRepository, times(1)).save(any(ChatMessage.class));
    }

    @Test
    void testSendMessage_WithReply() {
        ChatMessage parent = new ChatMessage();
        parent.setId(100L);
        when(userRepository.findById(recipient.getId())).thenReturn(Optional.of(recipient));
        when(conversationRepository.findDirectConversationBetweenUsers(sender, recipient))
                .thenReturn(Optional.of(conversation));
        when(messageRepository.findById(100L)).thenReturn(Optional.of(parent));
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(i -> i.getArguments()[0]);

        ChatMessage result = chatService.sendMessage(sender, recipient.getId(), "This is a reply", null, 100L, false, false);

        assertNotNull(result);
        assertEquals(parent, result.getParentMessage());
        verify(messageRepository, times(1)).save(any(ChatMessage.class));
    }

    @Test
    void testMarkMessagesAsSeen() {
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));
        ChatMessage msg = new ChatMessage();
        msg.setSender(recipient); // Someone else's message
        when(messageRepository.findByConversationOrderByTimestampAsc(conversation))
                .thenReturn(List.of(msg));
        when(readReceiptRepository.existsByMessageAndUser(any(), any())).thenReturn(false);
        when(readReceiptRepository.saveAll(any())).thenAnswer(i -> i.getArguments()[0]);

        List<MessageReadReceipt> result = chatService.markMessagesAsSeen(1L, sender);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(readReceiptRepository, atLeastOnce()).saveAll(any());
    }
}
