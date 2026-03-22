package com.jgnexus.dto;

import com.jgnexus.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class Dtos {

    // ── Auth ──────────────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RegisterRequest {
        @NotBlank private String fullName;
        @NotBlank @Size(min = 3, max = 50) private String username;
        @NotBlank @Email private String email;
        @NotBlank @Size(min = 6) private String password;
        private String collegeName;
        private String branch;
        private Integer yearOfStudy;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LoginRequest {
        @NotBlank private String usernameOrEmail;
        @NotBlank private String password;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private UserSummary user;
    }

    // ── User ──────────────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UserSummary {
        private Long id;
        private String username;
        private String fullName;
        private String profilePicture;
        private String collegeName;
        private String branch;
        private Boolean isVerified;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UserProfile {
        private Long id;
        private String username;
        private String fullName;
        private String email;
        private String bio;
        private String profilePicture;
        private String coverPhoto;
        private String collegeName;
        private String branch;
        private Integer yearOfStudy;
        private User.Role role;
        private Boolean isVerified;
        private long followersCount;
        private long followingCount;
        private long postsCount;
        private boolean isFollowing;
        private LocalDateTime createdAt;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UpdateProfileRequest {
        private String fullName;
        private String bio;
        private String collegeName;
        private String branch;
        private Integer yearOfStudy;
    }

    // ── Post ──────────────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CreatePostRequest {
        @NotBlank private String content;
        private List<String> imageUrls;
        private String tags;
        private String type;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PostResponse {
        private Long id;
        private String content;
        private List<String> imageUrls;
        private UserSummary author;
        private long likesCount;
        private long commentsCount;
        private boolean isLiked;
        private String tags;
        private String type;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    // ── Comment ───────────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CreateCommentRequest {
        @NotBlank private String content;
        private Long parentId;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CommentResponse {
        private Long id;
        private String content;
        private UserSummary author;
        private Long parentId;
        private List<CommentResponse> replies;
        private LocalDateTime createdAt;
    }

    // ── Chat ──────────────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ChatMessageDto {
        private Long id;
        private String conversationId;
        private UserSummary sender;
        private UserSummary receiver;
        private String content;
        private String type;
        private Boolean isRead;
        private LocalDateTime createdAt;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SendMessageRequest {
        @NotBlank private String content;
        @NotBlank private String type;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class WsChatMessage {
        private String conversationId;
        private Long senderId;
        private Long receiverId;
        private String content;
        private String type;
        private LocalDateTime timestamp;
    }

    // ── Notification ──────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class NotificationDto {
        private Long id;
        private UserSummary actor;
        private String type;
        private Long entityId;
        private String message;
        private Boolean isRead;
        private LocalDateTime createdAt;
    }

    // ── Generic ───────────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;

        public static <T> ApiResponse<T> ok(T data) {
            return ApiResponse.<T>builder().success(true).data(data).build();
        }

        public static <T> ApiResponse<T> ok(String message, T data) {
            return ApiResponse.<T>builder().success(true).message(message).data(data).build();
        }

        public static <T> ApiResponse<T> error(String message) {
            return ApiResponse.<T>builder().success(false).message(message).build();
        }
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PageResponse<T> {
        private List<T> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean last;
    }
}
