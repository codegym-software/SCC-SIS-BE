package com.example.sis.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class MeController {

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal OidcUser oidcUser) {
        if (oidcUser == null) {
            return Map.of("error", "No user info");
        }
        return Map.of(
                "username", oidcUser.getPreferredUsername(),
                "email", oidcUser.getEmail(),
                "claims", oidcUser.getClaims()
        );
    }

    @GetMapping("/public/ping")
    public Map<String, String> ping() {
        return Map.of("message", "pong (no auth required)");
    }
}
