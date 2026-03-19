package com.example.naim.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // Current logged-in user এর info
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal OAuth2User user) {
        if (user == null)
            return ResponseEntity.status(401).body(Map.of("authenticated", false));

        return ResponseEntity.ok(Map.of(
            "authenticated", true,
            "name",    user.getAttribute("name"),
            "email",   user.getAttribute("email"),
            "picture", user.getAttribute("picture")
        ));
    }

    // Simple true/false check (frontend auth guard এর জন্য)
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(@AuthenticationPrincipal OAuth2User user) {
        return ResponseEntity.ok(Map.of("authenticated", user != null));
    }
}
