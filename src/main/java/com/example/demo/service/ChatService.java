package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.model.MessageStatus;
import com.example.demo.model.MessageType;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private MessageReadReceiptRepository readReceiptRepository;

    public ChatMessage getChatMessage(Long id) {
        return chatMessageRepository.findById(id).orElse(null);
    }

    public List<Conversation> getUserConversations(User user) {
        List<Conversation> convs = conversationRepository.findAllByUserOrderByLastMessageTimeDesc(user);
        for (Conversation c : convs) {
            c.setUnread(chatMessageRepository.existsUnreadForUser(c, user));
        }
        return convs;
    }

    @Transactional
    public ChatMessage sendMessage(User sender, Long destinationId, String content, String mediaUrl, Long parentId,
            boolean isGroup, boolean isForwarded) {
        Conversation conv;
        if (isGroup) {
            conv = conversationRepository.findById(destinationId)
                    .orElseThrow(() -> new RuntimeException("Group not found"));
        } else {
            User recipient = userRepository.findById(destinationId)
                    .orElseThrow(() -> new RuntimeException("Recipient not found"));
            conv = conversationRepository.findDirectConversationBetweenUsers(sender, recipient)
                    .orElseGet(() -> {
                        Conversation newConv = new Conversation();
                        newConv.setType(Conversation.ConversationType.DIRECT);
                        List<User> participants = new ArrayList<>();
                        participants.add(sender);
                        participants.add(recipient);
                        newConv.setParticipants(participants);
                        newConv.setCreator(sender);
                        newConv.setStatus(Conversation.ConversationStatus.PENDING);
                        newConv.setLastMessage("Conversation started");
                        newConv.setLastMessageTime(LocalDateTime.now());
                        return conversationRepository.save(newConv);
                    });
        }

        ChatMessage message = new ChatMessage(conv, sender, content, mediaUrl);
        message.setVanish(conv.isVanishModeEnabled());
        message.setForwarded(isForwarded);

        // Determine MessageType
        if (mediaUrl != null && !mediaUrl.trim().isEmpty()) {
            message.setMessageType(MessageType.MEDIA);
            System.out.println("[DEBUG] ChatService.sendMessage: Saving MEDIA message with URL: " + mediaUrl);
        } else {
            message.setMessageType(MessageType.TEXT);
            System.out.println("[DEBUG] ChatService.sendMessage: Saving TEXT message");
        }

        if (parentId != null) {
            ChatMessage parent = chatMessageRepository.findById(parentId).orElse(null);
            message.setParentMessage(parent);
        }
        
        System.out.println("[DEBUG] ChatService.sendMessage: Saving message: content=" + content + ", mediaUrl=" + mediaUrl + ", type=" + message.getMessageType());
        ChatMessage saved = chatMessageRepository.save(message);
        System.out.println("[DEBUG] ChatService.sendMessage: Message saved with ID: " + saved.getId());

        conv.setLastMessage(content != null && !content.trim().isEmpty() ? content : "Media shared");
        conv.setLastMessageTime(LocalDateTime.now());
        conversationRepository.save(conv);

        return saved;
    }

    @Transactional
    public Conversation createGroup(String name, List<Long> userIds, User creator) {
        Conversation group = new Conversation();
        group.setName(name);
        group.setType(Conversation.ConversationType.GROUP);
        group.setCreator(creator);

        List<User> participants = new ArrayList<>();
        participants.add(creator);
        for (Long id : userIds) {
            userRepository.findById(id).ifPresent(participants::add);
        }
        group.setParticipants(participants);
        group.setLastMessage("Group created");
        group.setLastMessageTime(LocalDateTime.now());

        return conversationRepository.save(group);
    }

    @Transactional
    public Conversation toggleVanishMode(Long conversationId, boolean enabled) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        conv.setVanishModeEnabled(enabled);
        return conversationRepository.save(conv);
    }

    @Transactional
    public Conversation updateTheme(Long conversationId, String theme) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        conv.setTheme(theme);
        return conversationRepository.save(conv);
    }

    @Transactional
    public void cleanupVanishMessages(Long conversationId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        chatMessageRepository.deleteByConversationAndIsVanishTrueAndStatus(conv, MessageStatus.SEEN);
    }

    public List<ChatMessage> getChatHistory(Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        return chatMessageRepository.findByConversationOrderByTimestampAsc(conversation);
    }

    public List<ChatMessage> getConversationMedia(Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        return chatMessageRepository.findByConversationAndMediaUrlIsNotNullOrderByTimestampDesc(conversation);
    }

    @Transactional
    public List<MessageReadReceipt> markMessagesAsSeen(Long conversationId, User user) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        List<ChatMessage> history = chatMessageRepository.findByConversationOrderByTimestampAsc(conversation);
        List<MessageReadReceipt> newReceipts = new ArrayList<>();

        for (ChatMessage msg : history) {
            if (!msg.getSender().getId().equals(user.getId())
                    && !readReceiptRepository.existsByMessageAndUser(msg, user)) {
                newReceipts.add(new MessageReadReceipt(msg, user));
            }
        }
        return readReceiptRepository.saveAll(newReceipts);
    }

    public long getUnreadCount(User user) {
        List<Conversation> convs = conversationRepository.findAllByUserOrderByLastMessageTimeDesc(user);
        return chatMessageRepository.countUnreadForUserInConversations(convs, user);
    }

    @Transactional
    public void deleteMessage(Long messageId, User sender) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        if (!message.getSender().getId().equals(sender.getId())) {
            throw new RuntimeException("Unauthorized to delete this message");
        }
        chatMessageRepository.delete(message);
    }

    @Transactional
    public ChatMessage reactToMessage(Long messageId, String reaction, User reactor) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        Map<User, String> reactions = message.getReactions();
        if (reactions.containsKey(reactor) && reactions.get(reactor).equals(reaction)) {
            reactions.remove(reactor);
        } else {
            reactions.put(reactor, reaction);
        }

        return chatMessageRepository.save(message);
    }

    @Transactional
    public ChatMessage togglePin(Long messageId, User user) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        message.setPinned(!message.isPinned());
        message.setPinnedAt(message.isPinned() ? LocalDateTime.now() : null);

        return chatMessageRepository.save(message);
    }

    public List<ChatMessage> getPinnedMessages(Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        return chatMessageRepository.findByConversationAndIsPinnedTrueOrderByTimestampDesc(conversation);
    }

    @Transactional
    public ChatMessage sharePost(Long postId, Long conversationId, User user) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        ChatMessage msg = new ChatMessage();
        msg.setConversation(conv);
        msg.setSender(user);
        msg.setContent("Forwarded a post: " + post.getContent());
        msg.setMediaUrl(post.getMediaUrl());
        msg.setTimestamp(LocalDateTime.now());
        msg.setStatus(MessageStatus.SENT);

        return chatMessageRepository.save(msg);
    }

    @Transactional
    public Conversation acceptConversation(Long id, User user) {
        Conversation conv = conversationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        System.out.println("[DEBUG] ChatService.acceptConversation: convId=" + id + ", creator="
                + (conv.getCreator() != null ? conv.getCreator().getId() : "NULL") + ", userParams=" + user.getId());

        // Only recipient can accept
        if (conv.getCreator() != null && conv.getCreator().getId().equals(user.getId())) {
            throw new RuntimeException("Sender cannot accept their own request");
        }
        conv.setStatus(Conversation.ConversationStatus.ACCEPTED);
        return conversationRepository.save(conv);
    }

    @Transactional
    public void rejectConversation(Long id, User user) {
        Conversation conv = conversationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        System.out.println("[DEBUG] ChatService.rejectConversation: convId=" + id + ", creator="
                + (conv.getCreator() != null ? conv.getCreator().getId() : "NULL") + ", userParams=" + user.getId());

        if (conv.getCreator() != null && conv.getCreator().getId().equals(user.getId())) {
            throw new RuntimeException("Sender cannot reject their own request");
        }
        // Instead of deleting, we could mark as REJECTED, but usually deletion is
        // cleaner for chat requests
        conversationRepository.delete(conv);
    }
}
