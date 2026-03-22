package com.jgnexus.service;

import com.jgnexus.dto.Dtos.*;
import com.jgnexus.entity.Comment;
import com.jgnexus.entity.Post;
import com.jgnexus.entity.User;
import com.jgnexus.repository.CommentRepository;
import com.jgnexus.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserService userService;

    @Transactional
    @CacheEvict(value = {"feed", "trending"}, allEntries = true)
    public PostResponse createPost(String username, CreatePostRequest request) {
        User author = userService.findByUsername(username);
        Post post = Post.builder()
                .content(request.getContent())
                .imageUrls(request.getImageUrls() != null ? request.getImageUrls() : List.of())
                .author(author)
                .tags(request.getTags())
                .type(request.getType() != null
                        ? Post.PostType.valueOf(request.getType())
                        : Post.PostType.REGULAR)
                .build();
        return toPostResponse(postRepository.save(post), author);
    }

    @Cacheable(value = "feed", key = "#username + '_' + #page")
    public PageResponse<PostResponse> getFeed(String username, int page, int size) {
        User user = userService.findByUsername(username);
        Page<Post> posts = postRepository.findFeedPosts(user.getId(),
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return toPageResponse(posts, user, page, size);
    }

    @Cacheable(value = "trending", key = "#page")
    public PageResponse<PostResponse> getTrending(String username, int page, int size) {
        User user = userService.findByUsername(username);
        Page<Post> posts = postRepository.findTrendingPosts(PageRequest.of(page, size));
        return toPageResponse(posts, user, page, size);
    }

    public PageResponse<PostResponse> getUserPosts(Long userId, String viewerUsername, int page, int size) {
        User viewer = userService.findByUsername(viewerUsername);
        Page<Post> posts = postRepository
                .findByAuthorIdAndIsDeletedFalseOrderByCreatedAtDesc(userId,
                        PageRequest.of(page, size));
        return toPageResponse(posts, viewer, page, size);
    }

    public PostResponse getPost(Long postId, String username) {
        Post post = findById(postId);
        User viewer = userService.findByUsername(username);
        return toPostResponse(post, viewer);
    }

    @Transactional
    @CacheEvict(value = {"feed", "trending"}, allEntries = true)
    public boolean toggleLike(Long postId, String username) {
        Post post = findById(postId);
        User user = userService.findByUsername(username);
        boolean liked = post.getLikes().stream().anyMatch(u -> u.getId().equals(user.getId()));
        if (liked) {
            post.getLikes().remove(user);
        } else {
            post.getLikes().add(user);
        }
        postRepository.save(post);
        return !liked;
    }

    @Transactional
    public CommentResponse addComment(Long postId, String username, CreateCommentRequest request) {
        Post post = findById(postId);
        User author = userService.findByUsername(username);
        Comment parent = null;
        if (request.getParentId() != null) {
            parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent comment not found"));
        }
        Comment comment = Comment.builder()
                .content(request.getContent())
                .author(author)
                .post(post)
                .parent(parent)
                .build();
        Comment saved = commentRepository.save(comment);
        return toCommentResponse(saved);
    }

    public List<CommentResponse> getComments(Long postId) {
        return commentRepository
                .findByPostIdAndParentIsNullAndIsDeletedFalseOrderByCreatedAtAsc(postId)
                .stream()
                .map(this::toCommentResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = {"feed", "trending"}, allEntries = true)
    public void deletePost(Long postId, String username) {
        Post post = findById(postId);
        User user = userService.findByUsername(username);
        if (!post.getAuthor().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized");
        }
        post.setIsDeleted(true);
        postRepository.save(post);
    }

    public PageResponse<PostResponse> searchPosts(String query, String username, int page, int size) {
        User viewer = userService.findByUsername(username);
        Page<Post> posts = postRepository.searchPosts(query, PageRequest.of(page, size));
        return toPageResponse(posts, viewer, page, size);
    }

    private Post findById(Long id) {
        return postRepository.findById(id)
                .filter(p -> !p.getIsDeleted())
                .orElseThrow(() -> new RuntimeException("Post not found"));
    }

    private PostResponse toPostResponse(Post post, User viewer) {
        boolean isLiked = post.getLikes().stream()
                .anyMatch(u -> u.getId().equals(viewer.getId()));
        return PostResponse.builder()
                .id(post.getId())
                .content(post.getContent())
                .imageUrls(post.getImageUrls())
                .author(userService.toUserSummary(post.getAuthor()))
                .likesCount(post.getLikes().size())
                .commentsCount(commentRepository.countByPostIdAndIsDeletedFalse(post.getId()))
                .isLiked(isLiked)
                .tags(post.getTags())
                .type(post.getType().name())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    private CommentResponse toCommentResponse(Comment comment) {
        List<CommentResponse> replies = commentRepository
                .findByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(comment.getId())
                .stream().map(this::toCommentResponse).collect(Collectors.toList());
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .author(userService.toUserSummary(comment.getAuthor()))
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .replies(replies)
                .createdAt(comment.getCreatedAt())
                .build();
    }

    private PageResponse<PostResponse> toPageResponse(Page<Post> posts, User viewer, int page, int size) {
        return PageResponse.<PostResponse>builder()
                .content(posts.getContent().stream()
                        .map(p -> toPostResponse(p, viewer))
                        .collect(Collectors.toList()))
                .page(page).size(size)
                .totalElements(posts.getTotalElements())
                .totalPages(posts.getTotalPages())
                .last(posts.isLast())
                .build();
    }
}
