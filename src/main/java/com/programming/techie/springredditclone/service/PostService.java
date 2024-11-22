package com.programming.techie.springredditclone.service;

import com.programming.techie.springredditclone.dto.ImageDTO;
import com.programming.techie.springredditclone.dto.PostRequest;
import com.programming.techie.springredditclone.dto.PostResponse;
import com.programming.techie.springredditclone.exceptions.PostNotFoundException;
import com.programming.techie.springredditclone.exceptions.SubredditNotFoundException;
import com.programming.techie.springredditclone.mapper.PostMapper;
import com.programming.techie.springredditclone.model.Image;
import com.programming.techie.springredditclone.model.Post;
import com.programming.techie.springredditclone.model.Subreddit;
import com.programming.techie.springredditclone.model.User;
import com.programming.techie.springredditclone.repository.ImageRepository;
import com.programming.techie.springredditclone.repository.PostRepository;
import com.programming.techie.springredditclone.repository.SubredditRepository;
import com.programming.techie.springredditclone.repository.UserRepository;
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
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Service
@AllArgsConstructor
@Slf4j
@Transactional
public class PostService {

    private final PostRepository postRepository;
    private final SubredditRepository subredditRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final PostMapper postMapper;
    private final ImageRepository imageRepository;
    private final FileService fileService;


    public PostResponse save(PostRequest postRequest) {
        // Lưu bài viết từ thông tin trong PostRequest
        Post post = Post.builder()
                .postName(postRequest.getPostName())
                .description(postRequest.getDescription())
                .createdDate(Instant.now())
                .subreddit(subredditRepository.findByName(postRequest.getSubredditName()).get())
                .user(authService.getCurrentUser())
                .build();

        // Lưu bài viết vào cơ sở dữ liệu
        postRepository.save(post);
        return postMapper.mapToDto(post);
    }


    @Transactional(readOnly = true)
    public PostResponse getPost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException(id.toString()));
        return postMapper.mapToDto(post);
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getAllPosts() {
        return postRepository.findAll()
                .stream()
                .map(postMapper::mapToDto)
                .collect(toList());
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getPostsBySubreddit(Long subredditId) {
        Subreddit subreddit = subredditRepository.findById(subredditId)
                .orElseThrow(() -> new SubredditNotFoundException(subredditId.toString()));
        List<Post> posts = postRepository.findAllBySubreddit(subreddit);
        return posts.stream().map(postMapper::mapToDto).collect(toList());
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getPostsByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
        return postRepository.findByUser(user)
                .stream()
                .map(postMapper::mapToDto)
                .collect(toList());
    }
    public Page<PostResponse> getLatestPosts(Pageable pageable) {
        return postRepository.findAllByOrderByCreatedDateDesc(pageable)
                .map(postMapper::mapToDto);
    }

    public Image createPostImage(Long postId, ImageDTO build) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId.toString()));
        Image postImage = Image.builder()
                .imageUrl(build.getImageUrl())
                .post(post)
                .build();
        return imageRepository.save(postImage);
    }

    public Post getPostById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId.toString()));
    }

    public List<ImageDTO> getPostImages(Long postId) {
        // Ensure the post exists
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with ID: " + postId));

        // Query images associated with the post
        List<Image> postImages = imageRepository.findByPost(post);

        // Convert PostImage entities to DTOs
        return postImages.stream()
                .map(image -> ImageDTO.builder()
                        .imageUrl(image.getImageUrl())
                        .build())
                .toList();
    }
}
