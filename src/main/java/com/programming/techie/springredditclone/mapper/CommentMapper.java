package com.programming.techie.springredditclone.mapper;

import com.programming.techie.springredditclone.dto.CommentsDto;
import com.programming.techie.springredditclone.dto.UserDTO;
import com.programming.techie.springredditclone.model.Comment;
import com.programming.techie.springredditclone.model.Post;
import com.programming.techie.springredditclone.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "text", source = "commentsDto.text")
    @Mapping(target = "createdDate", expression = "java(java.time.Instant.now())")
    @Mapping(target = "post", source = "post")
    @Mapping(target = "user", source = "user")
    Comment map(CommentsDto commentsDto, Post post, User user);

    @Mapping(target = "postId", expression = "java(comment.getPost().getPostId())")
    @Mapping(target = "user", source = "user", qualifiedByName = "mapUserToUserDTO")
    @Mapping(target = "parentCommentId", expression = "java(comment.getParentComment() != null ? comment.getParentComment().getId() : null)")
    CommentsDto mapToDto(Comment comment);
    @org.mapstruct.Named("mapUserToUserDTO")
    default UserDTO mapUserToUserDTO(User user) {
        if (user == null) return null;
        return UserDTO.mapper(user);
    }
}
