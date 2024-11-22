package com.programming.techie.springredditclone.controller;

import com.programming.techie.springredditclone.dto.ImageDTO;
import com.programming.techie.springredditclone.dto.PostRequest;
import com.programming.techie.springredditclone.dto.PostResponse;
import com.programming.techie.springredditclone.model.Image;
import com.programming.techie.springredditclone.model.Post;
import com.programming.techie.springredditclone.service.FileService;
import com.programming.techie.springredditclone.service.PostService;
import lombok.AllArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.ResponseEntity.status;

@RestController
@RequestMapping("/api/posts")
@AllArgsConstructor
public class PostController {
    private final FileService fileService;
    private final PostService postService;

    @PostMapping
    public ResponseEntity<PostResponse> createPost(@RequestBody PostRequest postRequest) {
        PostResponse postResponse = postService.save(postRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(postResponse);
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<String> uploadFile(@RequestPart("file") MultipartFile file) {
        try {
            // Gọi FileService để lưu file
            String filePath = fileService.storeFile(file);

            // Trả về đường dẫn file đã lưu
            return ResponseEntity.ok("File uploaded successfully: " + filePath);
        } catch (RuntimeException e) {
            // Trả về lỗi nếu có vấn đề khi lưu file
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload file: " + e.getMessage());
        }
    }
    @PostMapping(value = "/{id}/upload-images", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadImages(
            @PathVariable("id") Long postId,
            @RequestPart("files") List<MultipartFile> files
    ) {
        try {
            // Validate and fetch the existing post
            Post existingPost = postService.getPostById(postId); // Ensure this method works correctly
            if (existingPost == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Post not found with ID: " + postId);
            }

            files = files == null ? new ArrayList<>() : files;

            if (files.size() > Image.MAXIMUM_IMAGES_PER_POST) {
                return ResponseEntity.badRequest().body("You can only upload a maximum of " + Image.MAXIMUM_IMAGES_PER_POST + " images.");
            }

            List<ImageDTO> postImageDTOs = new ArrayList<>();

            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    System.out.println("Skipping empty file...");
                    continue;
                }

                // File validation: size and type
                if (file.getSize() > 10 * 1024 * 1024) { // 10MB
                    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                            .body("File too large! Maximum size is 10MB.");
                }

                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                            .body("Invalid file type. Only image files are allowed.");
                }

                // Store the file and create PostImage entry
                String storedFilePath = fileService.storeFile(file);

                // Create a PostImage object and save it to the database
                Image postImage = postService.createPostImage(
                        existingPost.getPostId(),
                        ImageDTO.builder().imageUrl(storedFilePath).build()
                );

                postImageDTOs.add(ImageDTO.builder().imageUrl(postImage.getImageUrl()).build());
            }

            return ResponseEntity.ok(postImageDTOs);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }
    @GetMapping("/{id}/image_names")
    public ResponseEntity<?> getPostImages(@PathVariable("id") Long postId) {
        try {
            // Validate and fetch the existing post
            Post existingPost = postService.getPostById(postId);
            if (existingPost == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Post not found with ID: " + postId);
            }

            // Fetch images associated with the post
            List<ImageDTO> images = postService.getPostImages(postId);

            // Return the list of image URLs
            return ResponseEntity.ok(images);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }

    @GetMapping("/images/{filename}")
    public ResponseEntity<Resource> serveImage(@PathVariable String filename) {
        try {
            // Load the file from the "uploads" directory
            Path filePath = Paths.get("uploads").resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("Could not read the file: " + filename);
            }

            // Determine file type (e.g., "image/jpeg", "image/png")
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // Return the file as a response with the correct content type
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        }
    }

    @GetMapping
    public ResponseEntity<List<PostResponse>> getAllPosts() {
        return status(HttpStatus.OK).body(postService.getAllPosts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPostWithImages(@PathVariable("id") Long postId) {
        try {
            // Validate and fetch the post
            Post existingPost = postService.getPostById(postId);
            if (existingPost == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Post not found with ID: " + postId);
            }

            // Fetch images associated with the post
            List<ImageDTO> images = postService.getPostImages(postId);

            // Map images to their URLs
            List<String> imageUrls = images.stream()
                    .map(ImageDTO::getImageUrl) // Assuming ImageDTO has a `getUrl()` method
                    .collect(Collectors.toList());

            // Create a unified response DTO
            PostResponse response = PostResponse.builder()
                    .id(existingPost.getPostId())
                    .postName(existingPost.getPostName())
                    .description(existingPost.getDescription())
                    .userName(existingPost.getUser().getUsername())
//                    .timestamp(LocalDateTime.from(existingPost.getCreatedDate()))
                    .imageUrls(imageUrls) // Add the image URLs
                    .build();

            // Return the response
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }


    @GetMapping(params = "subredditId")
    public ResponseEntity<List<PostResponse>> getPostsBySubreddit(@RequestParam Long subredditId) {
        return status(HttpStatus.OK).body(postService.getPostsBySubreddit(subredditId));
    }

    @GetMapping(params = "username")
    public ResponseEntity<List<PostResponse>> getPostsByUsername(@RequestParam String username) {
        return status(HttpStatus.OK).body(postService.getPostsByUsername(username));
    }
    @GetMapping("/latest")
    public ResponseEntity<Page<PostResponse>> getLatestPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(postService.getLatestPosts(pageable));
    }
}
