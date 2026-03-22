package com.example.naim.controller;

import com.example.naim.model.User;
import com.example.naim.repository.UserRepository;
import com.example.naim.security.JwtUtils;
import com.example.naim.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import com.example.naim.dto.LoginRequest;
import com.example.naim.dto.SignupRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import com.example.naim.service.EmailService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import com.example.naim.service.RefreshTokenService;
import com.example.naim.model.RefreshToken;
import java.util.UUID;
import java.util.Optional;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest signupRequest) {
        if (userRepository.findByEmail(signupRequest.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is already taken!"));
        }

        String token = UUID.randomUUID().toString();
        User user = User.builder()
                .email(signupRequest.getEmail())
                .password(passwordEncoder.encode(signupRequest.getPassword()))
                .fullName(signupRequest.getFullName())
                .provider("LOCAL")
                .enabled(false)
                .verificationToken(token)
                .build();

        userRepository.save(user);
        
        try {
            emailService.sendVerificationEmail(user.getEmail(), token);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("message", "User registered, but failed to send verification email. Please contact support."));
        }

        return ResponseEntity.ok(Map.of("message", "User registered successfully! Please check your email to verify your account."));
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyUser(@RequestParam String token) {
        Optional<User> userOptional = userRepository.findByVerificationToken(token);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, "/login.html?error=invalid_token")
                    .build();
        }

        User user = userOptional.get();
        user.setEnabled(true);
        user.setVerificationToken(null);
        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, "/login.html?verified=true")
                .build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest, 
                                            HttpServletRequest request, 
                                            HttpServletResponse response) {
        
        Optional<User> userOptional = userRepository.findByEmail(loginRequest.getEmail());
        if (userOptional.isPresent() && !userOptional.get().isEnabled()) {
            return ResponseEntity.status(403).body(Map.of("message", "Email not verified. Please check your inbox."));
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        String jwt = jwtUtils.generateJwtToken(authentication);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userPrincipal.getUser().getId());

        // Add cookie for browser-based navigation
        Cookie cookie = new Cookie("JWT-TOKEN", jwt);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to true in production with HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(24 * 60 * 60); // 24 hours
        response.addCookie(cookie);

        return ResponseEntity.ok(Map.of(
                "token", jwt,
                "refreshToken", refreshToken.getToken(),
                "email", userPrincipal.getUsername(),
                "fullName", userPrincipal.getUser().getFullName(),
                "picture", userPrincipal.getUser().getPictureUrl() != null ? userPrincipal.getUser().getPictureUrl() : ""
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("authenticated", false));
        
        User user = principal.getUser();
        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "id", user.getId(),
                "email", user.getEmail(),
                "name", user.getFullName(),
                "picture", user.getPictureUrl() != null ? user.getPictureUrl() : "",
                "provider", user.getProvider()
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<?> status(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(Map.of("authenticated", principal != null));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String requestRefreshToken = request.get("refreshToken");

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String token = jwtUtils.generateTokenFromUsername(user.getEmail());
                    return ResponseEntity.ok(Map.of(
                            "token", token,
                            "refreshToken", requestRefreshToken
                    ));
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        
        // Invalidate Session
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // Clear JWT Cookie
        Cookie cookie = new Cookie("JWT-TOKEN", null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

}
