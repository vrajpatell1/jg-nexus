package com.jgnexus.controller;

import com.jgnexus.dto.Dtos.*;
import com.jgnexus.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{username}/profile")
    public ResponseEntity<ApiResponse<UserProfile>> getProfile(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails) {
        String viewer = userDetails != null ? userDetails.getUsername() : username;
        return ResponseEntity.ok(ApiResponse.ok(userService.getProfile(username, viewer)));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfile>> updateProfile(
            @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(
                userService.updateProfile(userDetails.getUsername(), request)));
    }

    @PostMapping("/{userId}/follow")
    public ResponseEntity<ApiResponse<Boolean>> toggleFollow(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean following = userService.toggleFollow(userDetails.getUsername(), userId);
        String msg = following ? "Followed successfully" : "Unfollowed successfully";
        return ResponseEntity.ok(ApiResponse.ok(msg, following));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<UserSummary>>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(userService.searchUsers(q, page, size)));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<List<UserSummary>>> suggestions(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(
                userService.getSuggestedUsers(userDetails.getUsername())));
    }

    @GetMapping("/{userId}/followers")
    public ResponseEntity<ApiResponse<List<UserSummary>>> getFollowers(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getFollowers(userId)));
    }

    @GetMapping("/{userId}/following")
    public ResponseEntity<ApiResponse<List<UserSummary>>> getFollowing(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getFollowing(userId)));
    }
}
