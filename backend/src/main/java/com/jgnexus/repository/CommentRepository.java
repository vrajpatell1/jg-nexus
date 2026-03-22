package com.jgnexus.repository;

import com.jgnexus.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostIdAndParentIsNullAndIsDeletedFalseOrderByCreatedAtAsc(Long postId);
    List<Comment> findByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(Long parentId);
    long countByPostIdAndIsDeletedFalse(Long postId);
}
