package com.michi.oauth.controller;

import com.michi.oauth.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class OAuthController {

    private final UserService userService;

    public OAuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/user")
    public ResponseEntity<?> user(
            @RequestHeader(value = "User-Id", defaultValue = "") String id,
            @RequestHeader(value = "User-Name", defaultValue = "Invitado") String name,
            @RequestHeader(value = "User-Role", defaultValue = "ROLE_USER") String role,
            @RequestHeader(value = "User-Picture", defaultValue = "") String picture,
            @RequestHeader(value = "User-Email", defaultValue = "") String email
    ) {

        List<String> favoriteProducts = id.isBlank() ? List.of() : userService.favoriteProducts(id);

        return ResponseEntity.ok(Map.of(
                "id", id,
                "name",    name,
                "role",    role,
                "picture", picture,
                "email",   email,
                "favoriteProducts", favoriteProducts
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        ResponseCookie deleteCookie = ResponseCookie.from("token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(Map.of("message", "Sesión cerrada correctamente"));
    }

    @PatchMapping ("/user/{id}")
    public ResponseEntity<String> markFavorite(@PathVariable String id, @RequestParam String productId,
                                               @RequestHeader(value = "User-Id", defaultValue = "") String userId) {
        checkOwner(id, userId);
        userService.markFavorite(id, productId);
        return ResponseEntity.ok("¡Se ha marcado el producto como favorito!");
    }

    @DeleteMapping("/user/{id}")
    public ResponseEntity<String> unmarkFavorite(@PathVariable String id, @RequestParam String productId,
                                                 @RequestHeader(value = "User-Id", defaultValue = "") String userId) {
        checkOwner(id, userId);
        userService.unmarkFavorite(id, productId);
        return ResponseEntity.ok("Se ha desmarcado el producto como favorito");
    }

    private void checkOwner(String id, String userId) {
        if (userId.isBlank() || !userId.equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes modificar los favoritos de otro usuario");
        }
    }

}