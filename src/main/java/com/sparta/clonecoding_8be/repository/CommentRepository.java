package com.sparta.clonecoding_8be.repository;

import com.sparta.clonecoding_8be.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository  extends JpaRepository<Comment, Long> {
    List<Comment> findAllByPostId(Long postId);

    void deleteByPostId(Long id);
}
