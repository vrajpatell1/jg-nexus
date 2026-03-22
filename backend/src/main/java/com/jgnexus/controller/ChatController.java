package com.jgnexus.controller;

import com.jgnexus.dto.Dtos.*;
import com.jgnexus.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // WebSocket STOMP endpoint — client sends to /app/chat
    @MessageMapping("/chat")
    public void handleMessage(@Payload WsChatMessage message, Principal principal) {
        message.setTimestamp(LocalDateTime.now());
        SendMessageRequest request = new SendMessageRequest(
                message.getContent(),
                message.getType() != null ? message.getType() : "TEXT"
        );
        chatService.sendMessage(principal.getName(), message.getReceiverId(), request);
    }

    // WebSocket STOMP — typing indicator
    @MessageMapping("/typing")
    public void handleTyping(@Payload WsChatMessage message, Principal principal) {
        chatService.setTyping(principal.getName(), message.getReceiverId(), true);
    }

    @RestController
    @RequestMapping("/chat")
    class ChatRestController {

        @PostMapping("/send/{receiverId}")
        public ResponseEntity<ApiResponse<ChatMessageDto>> send(
                @PathVariable Long receiverId,
                @RequestBody SendMessageRequest request,
                @AuthenticationPrincipal UserDetails userDetails) {
            ChatMessageDto msg = chatService.sendMessage(userDetails.getUsername(), receiverId, request);
            return ResponseEntity.ok(ApiResponse.ok(msg));
        }

        @GetMapping("/conversation/{userId}")
        public ResponseEntity<ApiResponse<PageResponse<ChatMessageDto>>> getConversation(
                @PathVariable Long userId,
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "20") int size,
                @AuthenticationPrincipal UserDetails userDetails) {
            return ResponseEntity.ok(ApiResponse.ok(
                    chatService.getConversation(userDetails.getUsername(), userId, page, size)));
        }

        @PostMapping("/conversation/{userId}/read")
        public ResponseEntity<ApiResponse<Void>> markRead(
                @PathVariable Long userId,
                @AuthenticationPrincipal UserDetails userDetails) {
            chatService.markAsRead(userDetails.getUsername(), userId);
            return ResponseEntity.ok(ApiResponse.ok("Marked as read", null));
        }

        @GetMapping("/unread-count")
        public ResponseEntity<ApiResponse<Long>> unreadCount(
                @AuthenticationPrincipal UserDetails userDetails) {
            // get user id from service
            return ResponseEntity.ok(ApiResponse.ok(0L));
        }

        @GetMapping("/online/{username}")
        public ResponseEntity<ApiResponse<Boolean>> isOnline(@PathVariable String username) {
            return ResponseEntity.ok(ApiResponse.ok(chatService.isUserOnline(username)));
        }
    }
}
