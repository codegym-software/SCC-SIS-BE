// src/main/java/com/example/sis/controller/AuthController.java
package com.example.sis.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> profile(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> body = new HashMap<>();
        body.put("sub", jwt.getClaimAsString("sub"));
        body.put("email", jwt.getClaimAsString("email"));
        body.put("preferred_username", jwt.getClaimAsString("preferred_username"));
        body.put("name", jwt.getClaimAsString("name"));
        body.put("iat", toIso(jwt.getClaim("iat")));
        body.put("exp", toIso(jwt.getClaim("exp")));
        return ResponseEntity.ok(body);
    }

    private String toIso(Object epochSeconds) {
        if (epochSeconds instanceof Number n) {
            return Instant.ofEpochSecond(n.longValue())
                    .atOffset(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
        return null;
    }
}
