package com.programming.techie.springredditclone.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String username;
    private String password;
    private String email;
    private Instant created;
    private boolean enabled;

    // Thêm các thuộc tính mới
    private String firstName;          // Họ
    private String lastName;           // Tên
    private String phoneNumber;        // Số điện thoại
    private Instant birthday;          // Ngày sinh
    private String role;               // Vai trò người dùng (ADMIN, USER, ...)
    private Instant lastLogin;         // Lần đăng nhập cuối
    private String address;            // Địa chỉ

    @OneToOne
    @JoinColumn(name = "profile_picture_url_id")
    private ImageUser profilePictureUrl;  // URL ảnh đại diện
}
