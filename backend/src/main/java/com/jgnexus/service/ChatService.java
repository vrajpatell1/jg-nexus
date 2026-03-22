package com.jgnexus.service;

import com.jgnexus.dto.Dtos.*;
import com.jgnexus.entity.ChatMessage;
import com.jgnexus.entity.User;
import com.jgnexus.repository.ChatMessageRepository;
import com.jgnexus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ONLINE_KEY = "online:";
    private static final String TYPING_KEY = "typing:";

    @Transactional
    public ChatMessageDto sendMessage(String senderUsername, Long receiverId, SendMessageRequest request) {
        User sender = userService.findByUsername(senderUsername);
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        String conversationId = buildConversationId(sender.getId(), receiverId);

        ChatMessage message = ChatMessage.builder()
                .conversationId(conversationId)
                .sender(sender)
                .receiver(receiver)
                .content(request.getContent())
                .type(ChatMessage.MessageType.valueOf(request.getType()))
                .build();

        ChatMessage saved = chatMessageRepository.save(message);
        ChatMessageDto dto = toDto(saved);

        // Push via WebSocket to receiver
        messagingTemplate.convertAndSendToUser(
                receiver.getUsername(),
                "/queue/messages",
                dto
        );

        return dto;
    }

    public PageResponse<ChatMessageDto> getConversation(String username, Long otherUserId, int page, int size) {
        User user = userService.findByUsername(username);
        String conversationId = buildConversationId(user.getId(), otherUserId);

        Page<ChatMessage> messages = chatMessageRepository
                .findByConversationIdOrderByCreatedAtDesc(
                        conversationId,
                        PageRequest.of(page, size, Sort.by("createdAt").descending())
                );

        List<ChatMessageDto> dtos = messages.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return PageResponse.<ChatMessageDto>builder()
                .content(dtos)
                .page(page).size(size)
                .totalElements(messages.getTotalElements())
                .totalPages(messages.getTotalPages())
                .last(messages.isLast())
                .build();
    }

    @Transactional
    public void markAsRead(String username, Long otherUserId) {
        User user = userService.findByUsername(username);
        String conversationId = buildConversationId(user.getId(), otherUserId);
        List<ChatMessage> unread = chatMessageRepository
                .findByConversationIdOrderByCreatedAtDesc(conversationId, PageRequest.of(0, 100))
                .getContent()
                .stream()
                .filter(m -> m.getReceiver().getId().equals(user.getId()) && !m.getIsRead())
                .collect(Collectors.toList());
        unread.forEach(m -> m.setIsRead(true));
        chatMessageRepository.saveAll(unread);
    }

    public void setUserOnline(String username, boolean online) {
        if (online) {
            redisTemplate.opsForValue().set(ONLINE_KEY + username, "true", 5, TimeUnit.MINUTES);
        } else {
            redisTemplate.delete(ONLINE_KEY + username);
        }
    }

    public boolean isUserOnline(String username) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(ONLINE_KEY + username));
    }

    public void setTyping(String username, Long conversationPartnerId, boolean typing) {
        String key = TYPING_KEY + username + ":" + conversationPartnerId;
        if (typing) {
            redisTemplate.opsForValue().set(key, "true", 10, TimeUnit.SECONDS);
        } else {
            redisTemplate.delete(key);
        }
    }

    public long countUnread(Long userId) {
        return chatMessageRepository.countUnreadMessages(userId);
    }

    public String buildConversationId(Long userA, Long userB) {
        long min = Math.min(userA, userB);
        long max = Math.max(userA, userB);
        return "conv_" + min + "_" + max;
    }

    private ChatMessageDto toDto(ChatMessage msg) {
        return ChatMessageDto.builder()
                .id(msg.getId())
                .conversationId(msg.getConversationId())
                .sender(userService.toUserSummary(msg.getSender()))
                .receiver(userService.toUserSummary(msg.getReceiver()))
                .content(msg.getContent())
                .type(msg.getType().name())
                .isRead(msg.getIsRead())
                .createdAt(msg.getCreatedAt())
                .build();
    }
}
