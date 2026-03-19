package com.example.naim.controller;

import com.example.naim.model.User;
import com.example.naim.repository.UserRepository;
import com.example.naim.security.JwtUtils;
import com.example.naim.security.UserPrincipal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

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

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest signupRequest) {
        if (userRepository.findByEmail(signupRequest.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is already taken!"));
        }

        User user = User.builder()
                .email(signupRequest.getEmail())
                .password(passwordEncoder.encode(signupRequest.getPassword()))
                .fullName(signupRequest.getFullName())
                .provider("LOCAL")
                .build();

        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "User registered successfully!"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        return ResponseEntity.ok(Map.of(
                "token", jwt,
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

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    static class LoginRequest {
        private String email;
        private String password;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    static class SignupRequest {
        private String email;
        private String password;
        private String fullName;
    }
}
