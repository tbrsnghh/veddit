package com.programming.techie.springredditclone.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PostResponse{
    private Long id;
    private String postName;
    private String url;
    private String description;
    private UserDTO user;
    private String subredditName;
    private Integer voteCount;
    private Integer commentCount;
//    private LocalDateTime timestamp;
    private String duration;
    private boolean upVote;
    private boolean downVote;
    private List<String> imageUrls;
}
