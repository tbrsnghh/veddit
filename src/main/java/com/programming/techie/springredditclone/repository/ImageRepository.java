package com.programming.techie.springredditclone.repository;

import com.programming.techie.springredditclone.model.Image;
import com.programming.techie.springredditclone.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findByPost(Post post);
}

