package com.programming.techie.springredditclone.repository;

import com.programming.techie.springredditclone.model.Comment;
import com.programming.techie.springredditclone.model.Post;
import com.programming.techie.springredditclone.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostOrderByCreatedDateDesc(Post post);

    List<Comment> findByPostAndParentCommentIsNullOrderByCreatedDateDesc(Post post);

    List<Comment> findByParentCommentOrderByCreatedDateDesc(Comment parentComment);

    List<Comment> findAllByUserOrderByCreatedDateDesc(User user);
}
