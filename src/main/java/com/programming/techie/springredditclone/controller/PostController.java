package com.programming.techie.springredditclone.controller;

import com.programming.techie.springredditclone.dto.ImageDTO;
import com.programming.techie.springredditclone.dto.PostRequest;
import com.programming.techie.springredditclone.dto.PostResponse;
import com.programming.techie.springredditclone.model.Image;
import com.programming.techie.springredditclone.model.Post;
import com.programming.techie.springredditclone.responses.ApiResponse;
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

    // Tạo bài viết
    @PostMapping
    public ResponseEntity<ApiResponse<PostResponse>> createPost(@RequestBody PostRequest postRequest) {
        PostResponse postResponse = postService.save(postRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Post created successfully", postResponse));
    }

    // Tải lên hình ảnh cho bài viết
    @PostMapping(value = "/{id}/images", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<List<ImageDTO>>> uploadImages(
            @PathVariable("id") Long postId,
            @RequestPart("files") List<MultipartFile> files) {
        List<ImageDTO> images = postService.uploadImages(postId, files);
        return ResponseEntity.ok(new ApiResponse<>(true, "Images uploaded successfully", images));
    }

    // Lấy danh sách bài viết mới nhất
    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<Page<PostResponse>>> getLatestPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PostResponse> latestPosts = postService.getLatestPosts(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Latest posts fetched successfully", latestPosts));
    }

    // Lấy thông tin bài viết và danh sách hình ảnh
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostResponse>> getPostWithImages(@PathVariable("id") Long postId) {
        PostResponse response = postService.getPostWithImages(postId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Post fetched successfully", response));
    }

    // Phục vụ hình ảnh từ server
    @GetMapping("/images/{filename}")
    public ResponseEntity<Resource> serveImage(@PathVariable String filename) {
        Resource resource = fileService.loadFileAsResource(filename);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileService.getContentType(resource)))
                .body(resource);
    }
}



