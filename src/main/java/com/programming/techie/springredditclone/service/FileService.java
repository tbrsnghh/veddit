package com.programming.techie.springredditclone.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileService {
    private final String uploadDir;

    public FileService(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    public String storeFile(MultipartFile file) {
        try {
            // Tạo thư mục lưu file nếu chưa tồn tại
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Tạo tên file (có thể thêm UUID để tránh trùng)
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);

            // Lưu file vào thư mục
            Files.copy(file.getInputStream(), filePath);
            System.out.println("File saved to: " + filePath.toString());


            // Trả về đường dẫn của file
            return filePath.toString();
        } catch (IOException e) {
            throw new RuntimeException("Không thể lưu file: " + e.getMessage());
        }
    }
}


