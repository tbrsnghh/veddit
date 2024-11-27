package com.programming.techie.springredditclone.service;

import com.programming.techie.springredditclone.dto.CommentsDto;
import com.programming.techie.springredditclone.exceptions.PostNotFoundException;
import com.programming.techie.springredditclone.exceptions.SpringRedditException;
import com.programming.techie.springredditclone.mapper.CommentMapper;
import com.programming.techie.springredditclone.model.Comment;
import com.programming.techie.springredditclone.model.NotificationEmail;
import com.programming.techie.springredditclone.model.Post;
import com.programming.techie.springredditclone.model.User;
import com.programming.techie.springredditclone.repository.CommentRepository;
import com.programming.techie.springredditclone.repository.PostRepository;
import com.programming.techie.springredditclone.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class CommentService {
    private static final String POST_URL = "";
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final CommentMapper commentMapper;
    private final CommentRepository commentRepository;
    private final MailContentBuilder mailContentBuilder;
    private final MailService mailService;

    public void save(CommentsDto commentsDto) {
        Post post = postRepository.findById(commentsDto.getPostId())
                .orElseThrow(() -> new PostNotFoundException(commentsDto.getPostId().toString()));
        Comment comment = commentMapper.map(commentsDto, post, authService.getCurrentUser());

        if (commentsDto.getParentCommentId() != null) {
            Comment parentComment = commentRepository.findById(commentsDto.getParentCommentId())
                    .orElseThrow(() -> new RuntimeException("Parent comment not found"));
            comment.setParentComment(parentComment);
            parentComment.getSubComments().add(comment); // Thêm bình luận con vào danh sách bình luận con của bình luận cha
        }

        commentRepository.save(comment);

//        String message = mailContentBuilder.build(post.getUser().getUsername() + " posted a comment on your post." + POST_URL);
//        sendCommentNotification(message, post.getUser());
    }

    private void sendCommentNotification(String message, User user) {
        mailService.sendMail(new NotificationEmail(user.getUsername() + " Commented on your post", user.getEmail(), message));
    }

    public List<CommentsDto> getAllCommentsForPost(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new PostNotFoundException(postId.toString()));
        return commentRepository.findByPostOrderByCreatedDateDesc(post)
                .stream()
                .map(commentMapper::mapToDto).toList();
    }

    public List<CommentsDto> getAllCommentsForUser(String userName) {
        User user = userRepository.findByUsername(userName)
                .orElseThrow(() -> new UsernameNotFoundException(userName));
        return commentRepository.findAllByUserOrderByCreatedDateDesc(user)
                .stream()
                .map(commentMapper::mapToDto)
                .toList();
    }

    public boolean containsSwearWords(String comment) {
        if (comment.contains("shit")) {
            throw new SpringRedditException("Comments contains unacceptable language");
        }
        return false;
    }

    public List<CommentsDto> getAllLv1CommentsForPost(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new PostNotFoundException(postId.toString()));
        return commentRepository.findByPostAndParentCommentIsNullOrderByCreatedDateDesc(post)
                .stream()
                .map(commentMapper::mapToDto).toList();
    }

    public List<CommentsDto> getAllSubCommentsForComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new PostNotFoundException(commentId.toString()));
        return commentRepository.findByParentCommentOrderByCreatedDateDesc(comment)
                .stream()
                .map(commentMapper::mapToDto).toList();
    }
}
