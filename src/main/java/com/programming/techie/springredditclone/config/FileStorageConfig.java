package com.programming.techie.springredditclone.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileStorageConfig {
    @Value("${file.upload-dir}")
    private String uploadDir;

    @Bean
    public String fileUploadDir() {
        return uploadDir;
    }
}
