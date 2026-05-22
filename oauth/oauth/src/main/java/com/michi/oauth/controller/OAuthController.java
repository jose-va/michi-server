package com.michi.oauth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class OAuthController {

    @GetMapping
    public ResponseEntity<?> user(
            @RequestHeader(value = "User-Name", defaultValue = "Invitado") String name,
            @RequestHeader(value = "User-Role", defaultValue = "ROLE_USER") String role,
            @RequestHeader(value = "User-Picture", defaultValue = "") String picture,
            @RequestHeader(value = "User-Email", defaultValue = "") String email,
            @RequestHeader(value = "User-Id", defaultValue = "") String googleId
    ) {
        return ResponseEntity.ok(Map.of(
                "name",    name,
                "role",    role,
                "picture", picture,
                "email",   email,
                "googleId", googleId
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        ResponseCookie deleteCookie = ResponseCookie.from("token", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(Map.of("message", "Sesión cerrada correctamente"));
    }
}