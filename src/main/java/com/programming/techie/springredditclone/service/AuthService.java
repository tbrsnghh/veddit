package com.programming.techie.springredditclone.service;

import com.programming.techie.springredditclone.dto.*;
import com.programming.techie.springredditclone.exceptions.SpringRedditException;
import com.programming.techie.springredditclone.model.ImageUser;
import com.programming.techie.springredditclone.model.NotificationEmail;
import com.programming.techie.springredditclone.model.User;
import com.programming.techie.springredditclone.model.VerificationToken;
import com.programming.techie.springredditclone.repository.ImageUserRepository;
import com.programming.techie.springredditclone.repository.UserRepository;
import com.programming.techie.springredditclone.repository.VerificationTokenRepository;
import com.programming.techie.springredditclone.responses.ApiResponse;
import com.programming.techie.springredditclone.security.JwtProvider;
import io.github.classgraph.Resource;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
@Transactional
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final MailService mailService;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final FileService fileService;
    private final ImageUserRepository imageUserRepository;

    public void signup(RegisterRequest registerRequest) {
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setCreated(Instant.now());
        user.setEnabled(false);

        userRepository.save(user);

        String token = generateVerificationToken(user);
        System.out.println(token);
//        mailService.sendMail(new NotificationEmail("Please Activate your Account",
//                user.getEmail(), "Thank you for signing up to Spring Reddit, " +
//                "please click on the below url to activate your account : " +
//                "http://localhost:8080/api/auth/accountVerification/" + token));
    }

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        Jwt principal = (Jwt) SecurityContextHolder.
                getContext().getAuthentication().getPrincipal();
        return userRepository.findByUsername(principal.getSubject())
                .orElseThrow(() -> new UsernameNotFoundException("User name not found - " + principal.getSubject()));
    }

    private void fetchUserAndEnable(VerificationToken verificationToken) {
        String username = verificationToken.getUser().getUsername();
        User user = userRepository.findByUsername(username).orElseThrow(() -> new SpringRedditException("User not found with name - " + username));
        user.setEnabled(true);
        userRepository.save(user);
    }

    private String generateVerificationToken(User user) {
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);

        verificationTokenRepository.save(verificationToken);
        return token;
    }

    public void verifyAccount(String token) {
        Optional<VerificationToken> verificationToken = verificationTokenRepository.findByToken(token);
        fetchUserAndEnable(verificationToken.orElseThrow(() -> new SpringRedditException("Invalid Token")));
    }

    public AuthenticationResponse login(LoginRequest loginRequest) {
        Authentication authenticate = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(),
                loginRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authenticate);
        String token = jwtProvider.generateToken(authenticate);
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username - " + loginRequest.getUsername()));
        UserDTO userDTO = convertUserToUserDTO(user);
        return AuthenticationResponse.builder()
                .authenticationToken(token)
                .refreshToken(refreshTokenService.generateRefreshToken().getToken())
                .expiresAt(Instant.now().plusMillis(jwtProvider.getJwtExpirationInMillis()))
                .user(userDTO)
                .build();
    }

    private UserDTO convertUserToUserDTO(User user) {
        return UserDTO.builder()
                .email(user.getEmail())
                .username(user.getUsername())
                .created(user.getCreated())
                .userId(user.getUserId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .birthday(user.getBirthday())
                .profilePictureUrl(user.getProfilePictureUrl() != null ? user.getProfilePictureUrl().getFileName() : null)
                .address(user.getAddress())
                .enabled(user.isEnabled())
                .build();
    }

    public AuthenticationResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        refreshTokenService.validateRefreshToken(refreshTokenRequest.getRefreshToken());
        String token = jwtProvider.generateTokenWithUserName(refreshTokenRequest.getUsername());
        User user = userRepository.findByUsername(refreshTokenRequest.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username - " + refreshTokenRequest.getUsername()));
        UserDTO userDTO = convertUserToUserDTO(user);
        return AuthenticationResponse.builder()
                .authenticationToken(token)
                .refreshToken(refreshTokenRequest.getRefreshToken())
                .expiresAt(Instant.now().plusMillis(jwtProvider.getJwtExpirationInMillis()))
                .user(userDTO)
                .build();
    }

    public boolean isLoggedIn() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return !(authentication instanceof AnonymousAuthenticationToken) && authentication.isAuthenticated();
    }

    public ResponseEntity<String> uploadProfilePicture(MultipartFile file) {
        String filePath = fileService.storeFile(file);

        User user = getCurrentUser();
        ImageUser imageUser = ImageUser.builder()
                .fileName(filePath)
                .uploadedAt(Instant.now())
                .user(user).build();
        imageUserRepository.save(imageUser);
        user.setProfilePictureUrl(imageUser);
        userRepository.save(user);

        return ResponseEntity.ok().body("Profile Picture uploaded successfully!!");
    }

    public ResponseEntity<ApiResponse> getUserInfo() {
        User user = getCurrentUser();
        UserDTO userDTO = convertUserToUserDTO(user);
        return ResponseEntity.ok().body(ApiResponse.builder().data(userDTO).build());
    }
}