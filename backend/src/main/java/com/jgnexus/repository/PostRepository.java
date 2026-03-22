package com.jgnexus.repository;

import com.jgnexus.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByAuthorIdAndIsDeletedFalseOrderByCreatedAtDesc(Long authorId, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.author.id IN " +
           "(SELECT f.id FROM User u JOIN u.following f WHERE u.id = :userId) " +
           "AND p.isDeleted = false ORDER BY p.createdAt DESC")
    Page<Post> findFeedPosts(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.isDeleted = false ORDER BY SIZE(p.likes) DESC, p.createdAt DESC")
    Page<Post> findTrendingPosts(Pageable pageable);

    @Query("SELECT p FROM Post p WHERE (p.content LIKE %:query% OR p.tags LIKE %:query%) AND p.isDeleted = false")
    Page<Post> searchPosts(@Param("query") String query, Pageable pageable);
}
