package com.jgnexus.service;

import com.jgnexus.dto.Dtos.*;
import com.jgnexus.entity.Notification;
import com.jgnexus.entity.User;
import com.jgnexus.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    @Async
    public void send(User recipient, User actor, Notification.NotificationType type,
                     Long entityId, String message) {
        if (recipient.getId().equals(actor.getId())) return;

        Notification notification = Notification.builder()
                .recipient(recipient)
                .actor(actor)
                .type(type)
                .entityId(entityId)
                .message(message)
                .build();

        Notification saved = notificationRepository.save(notification);
        NotificationDto dto = toDto(saved);

        messagingTemplate.convertAndSendToUser(
                recipient.getUsername(),
                "/queue/notifications",
                dto
        );
    }

    public PageResponse<NotificationDto> getNotifications(String username, int page, int size) {
        User user = userService.findByUsername(username);
        Page<Notification> notifications = notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(page, size));

        List<NotificationDto> dtos = notifications.getContent().stream()
                .map(this::toDto).collect(Collectors.toList());

        return PageResponse.<NotificationDto>builder()
                .content(dtos).page(page).size(size)
                .totalElements(notifications.getTotalElements())
                .totalPages(notifications.getTotalPages())
                .last(notifications.isLast())
                .build();
    }

    public long countUnread(String username) {
        User user = userService.findByUsername(username);
        return notificationRepository.countByRecipientIdAndIsReadFalse(user.getId());
    }

    @Transactional
    public void markAllRead(String username) {
        User user = userService.findByUsername(username);
        notificationRepository.markAllAsRead(user.getId());
    }

    private NotificationDto toDto(Notification n) {
        return NotificationDto.builder()
                .id(n.getId())
                .actor(n.getActor() != null ? userService.toUserSummary(n.getActor()) : null)
                .type(n.getType().name())
                .entityId(n.getEntityId())
                .message(n.getMessage())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
