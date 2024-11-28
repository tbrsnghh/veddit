package com.programming.techie.springredditclone.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentsDto {
    private Long id;
    private Long postId;

    // Thay đổi kiểu dữ liệu từ Instant sang LocalDateTime
    private Instant createdDate;

    @NotBlank(message = "Text cannot be blank")
    @Size(max = 500, message = "Text cannot exceed 500 characters") // Giới hạn độ dài
    private String text;

    private UserDTO user;
    // Nếu cần, có thể thêm thông tin người dùng khác ở đây
    private Long parentCommentId;
}