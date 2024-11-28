package com.programming.techie.springredditclone.service;

import com.github.marlonlom.utilities.timeago.TimeAgo;
import com.programming.techie.springredditclone.dto.ImageDTO;
import com.programming.techie.springredditclone.dto.PostRequest;
import com.programming.techie.springredditclone.dto.PostResponse;
import com.programming.techie.springredditclone.exceptions.PostNotFoundException;
import com.programming.techie.springredditclone.exceptions.SubredditNotFoundException;
import com.programming.techie.springredditclone.mapper.PostMapper;
import com.programming.techie.springredditclone.model.*;
import com.programming.techie.springredditclone.repository.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.programming.techie.springredditclone.model.VoteType.DOWNVOTE;
import static com.programming.techie.springredditclone.model.VoteType.UPVOTE;
import static java.util.stream.Collectors.toList;

//@Service
//@AllArgsConstructor
//@Slf4j
//@Transactional
//public class PostService {
//
//    private final PostRepository postRepository;
//    private final SubredditRepository subredditRepository;
//    private final UserRepository userRepository;
//    private final AuthService authService;
//    private final PostMapper postMapper;
//    private final ImageRepository imageRepository;
//    private final FileService fileService;
//
//
//    public PostResponse save(PostRequest postRequest) {
//        // Lưu bài viết từ thông tin trong PostRequest
//        Post post = Post.builder()
//                .postName(postRequest.getPostName())
//                .description(postRequest.getDescription())
//                .createdDate(Instant.now())
//                .subreddit(subredditRepository.findByName(postRequest.getSubredditName()).get())
//                .user(authService.getCurrentUser())
//                .build();
//
//        // Lưu bài viết vào cơ sở dữ liệu
//        postRepository.save(post);
//        return postMapper.mapToDto(post);
//    }
//
//
//    @Transactional(readOnly = true)
//    public PostResponse getPost(Long id) {
//        Post post = postRepository.findById(id)
//                .orElseThrow(() -> new PostNotFoundException(id.toString()));
//        return postMapper.mapToDto(post);
//    }
//
//    @Transactional(readOnly = true)
//    public List<PostResponse> getAllPosts() {
//        return postRepository.findAll()
//                .stream()
//                .map(postMapper::mapToDto)
//                .collect(toList());
//    }
//
//    @Transactional(readOnly = true)
//    public List<PostResponse> getPostsBySubreddit(Long subredditId) {
//        Subreddit subreddit = subredditRepository.findById(subredditId)
//                .orElseThrow(() -> new SubredditNotFoundException(subredditId.toString()));
//        List<Post> posts = postRepository.findAllBySubreddit(subreddit);
//        return posts.stream().map(postMapper::mapToDto).collect(toList());
//    }
//
//    @Transactional(readOnly = true)
//    public List<PostResponse> getPostsByUsername(String username) {
//        User user = userRepository.findByUsername(username)
//                .orElseThrow(() -> new UsernameNotFoundException(username));
//        return postRepository.findByUser(user)
//                .stream()
//                .map(postMapper::mapToDto)
//                .collect(toList());
//    }
//    public Page<PostResponse> getLatestPosts(Pageable pageable) {
//        return postRepository.findAllByOrderByCreatedDateDesc(pageable)
//                .map(postMapper::mapToDto);
//    }
//
//    public Image createPostImage(Long postId, ImageDTO build) {
//        Post post = postRepository.findById(postId)
//                .orElseThrow(() -> new PostNotFoundException(postId.toString()));
//        Image postImage = Image.builder()
//                .imageUrl(build.getImageUrl())
//                .post(post)
//                .build();
//        return imageRepository.save(postImage);
//    }
//
//    public Post getPostById(Long postId) {
//        return postRepository.findById(postId)
//                .orElseThrow(() -> new PostNotFoundException(postId.toString()));
//    }
//
//    public List<ImageDTO> getPostImages(Long postId) {
//        // Ensure the post exists
//        Post post = postRepository.findById(postId)
//                .orElseThrow(() -> new RuntimeException("Post not found with ID: " + postId));
//
//        // Query images associated with the post
//        List<Image> postImages = imageRepository.findByPost(post);
//
//        // Convert PostImage entities to DTOs
//        return postImages.stream()
//                .map(image -> ImageDTO.builder()
//                        .imageUrl(image.getImageUrl())
//                        .build())
//                .toList();
//    }
//}
@Service
@AllArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final ImageRepository imageRepository;
    private final FileService fileService;
    private final VoteRepository voteRepository;
    private final AuthService authService;
    private final SubredditRepository subredditRepository;

    public PostResponse save(PostRequest postRequest) {
        Post post = Post.builder()
                .description(postRequest.getDescription())
                .postName(postRequest.getPostName())
                .createdDate(Instant.now())
                .build();

        System.out.println(post);
        postRepository.save(post);
        return mapToDto(post);
    }

    public List<ImageDTO> uploadImages(Long postId, List<MultipartFile> files) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        List<ImageDTO> imageDTOs = new ArrayList<>();
        for (MultipartFile file : files) {
            String filePath = fileService.storeFile(file);
            Image image = new Image();
            image.setPost(post);
            image.setImageUrl(filePath);
            imageRepository.save(image);
            imageDTOs.add(new ImageDTO(filePath));
        }
        return imageDTOs;
    }

    public Page<PostResponse> getLatestPosts(Pageable pageable) {
        return postRepository.findAllByOrderByCreatedDateDesc(pageable)
                .map(this::mapToDto);
    }

    public PostResponse getPostWithImages(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        List<String> imageUrls = imageRepository.findByPost(post)
                .stream().map(Image::getImageUrl).collect(Collectors.toList());
        PostResponse postResponse = mapToDto(post);
        postResponse.setImageUrls(imageUrls);
        return postResponse;
    }

    private PostResponse mapToDto(Post post) {
        // Retrieve the current user's vote on this post, if it exists
        Vote vote = voteRepository.findTopByPostAndUserOrderByVoteIdDesc(post, authService.getCurrentUser())
                .orElse(null);

        // Determine upVote and downVote status based on the retrieved vote
        boolean upVote = vote != null && vote.getVoteType().equals(UPVOTE);
        boolean downVote = vote != null && vote.getVoteType().equals(DOWNVOTE);

        List<String> images = imageRepository.findByPost(post).stream().map(Image::getImageUrl).collect(Collectors.toList());
        // Map the Post entity to a PostResponse DTO
        return PostResponse.builder()
                .id(post.getPostId())
                .postName(post.getPostName())
                .description(post.getDescription())
                .userName(Optional.ofNullable(post.getUser()).map(User::getUsername).orElse(""))
                .upVote(upVote)
                .downVote(downVote)
                .subredditName(Optional.ofNullable(post.getSubreddit()).map(Subreddit::getName).orElse(""))
                .voteCount(post.getVoteCount())
                .imageUrls(images)
//                .commentCount(post.getCommentCount())
                .duration(getDuration(post))
                .build();
    }

    private String getDuration(Post post) {
        return TimeAgo.using(post.getCreatedDate().toEpochMilli());
    }

}
