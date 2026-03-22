package com.jgnexus.service;

import com.jgnexus.dto.Dtos.*;
import com.jgnexus.entity.User;
import com.jgnexus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Cacheable(value = "userProfile", key = "#username")
    public UserProfile getProfile(String username, String currentUsername) {
        User user = findByUsername(username);
        User currentUser = findByUsername(currentUsername);

        boolean isFollowing = user.getFollowers().stream()
                .anyMatch(f -> f.getId().equals(currentUser.getId()));

        return UserProfile.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .bio(user.getBio())
                .profilePicture(user.getProfilePicture())
                .coverPhoto(user.getCoverPhoto())
                .collegeName(user.getCollegeName())
                .branch(user.getBranch())
                .yearOfStudy(user.getYearOfStudy())
                .role(user.getRole())
                .isVerified(user.getIsVerified())
                .followersCount(user.getFollowers().size())
                .followingCount(user.getFollowing().size())
                .postsCount(user.getPosts().size())
                .isFollowing(isFollowing)
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Transactional
    @CacheEvict(value = "userProfile", key = "#username")
    public UserProfile updateProfile(String username, UpdateProfileRequest request) {
        User user = findByUsername(username);
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getCollegeName() != null) user.setCollegeName(request.getCollegeName());
        if (request.getBranch() != null) user.setBranch(request.getBranch());
        if (request.getYearOfStudy() != null) user.setYearOfStudy(request.getYearOfStudy());
        userRepository.save(user);
        return getProfile(username, username);
    }

    @Transactional
    @CacheEvict(value = "userProfile", allEntries = true)
    public boolean toggleFollow(String currentUsername, Long targetUserId) {
        User currentUser = findByUsername(currentUsername);
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (currentUser.getId().equals(targetUserId)) {
            throw new RuntimeException("Cannot follow yourself");
        }

        boolean isFollowing = targetUser.getFollowers().stream()
                .anyMatch(f -> f.getId().equals(currentUser.getId()));

        if (isFollowing) {
            targetUser.getFollowers().remove(currentUser);
        } else {
            targetUser.getFollowers().add(currentUser);
        }
        userRepository.save(targetUser);
        return !isFollowing;
    }

    public PageResponse<UserSummary> searchUsers(String query, int page, int size) {
        Page<User> result = userRepository.searchUsers(query, PageRequest.of(page, size));
        List<UserSummary> summaries = result.getContent().stream()
                .map(this::toUserSummary)
                .collect(Collectors.toList());
        return PageResponse.<UserSummary>builder()
                .content(summaries)
                .page(page)
                .size(size)
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    public List<UserSummary> getSuggestedUsers(String username) {
        User user = findByUsername(username);
        return userRepository.findByCollege(user.getCollegeName(), user.getId())
                .stream()
                .filter(u -> !u.getFollowers().contains(user))
                .limit(5)
                .map(this::toUserSummary)
                .collect(Collectors.toList());
    }

    public List<UserSummary> getFollowers(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getFollowers().stream()
                .map(this::toUserSummary)
                .collect(Collectors.toList());
    }

    public List<UserSummary> getFollowing(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getFollowing().stream()
                .map(this::toUserSummary)
                .collect(Collectors.toList());
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    public UserSummary toUserSummary(User user) {
        return UserSummary.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .profilePicture(user.getProfilePicture())
                .collegeName(user.getCollegeName())
                .branch(user.getBranch())
                .isVerified(user.getIsVerified())
                .build();
    }
}
