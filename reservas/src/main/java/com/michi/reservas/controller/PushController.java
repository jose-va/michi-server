package com.michi.reservas.controller;

import com.michi.reservas.dto.PushSubscribeDTO;
import com.michi.reservas.model.PushSubscription;
import com.michi.reservas.push.PushNotificationService;
import com.michi.reservas.push.PushProperties;
import com.michi.reservas.repository.PushSubscriptionRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/reservations/push")
public class PushController {

    private final PushSubscriptionRepository subscriptionRepository;
    private final PushNotificationService pushNotificationService;
    private final PushProperties pushProperties;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @GetMapping("/public-key")
    public Map<String, String> publicKey() {
        String key = pushProperties.publicKey() == null ? "" : pushProperties.publicKey();
        return Map.of("publicKey", key);
    }

    @PostMapping("/subscriptions")
    public ResponseEntity<Void> subscribe(
            @Valid @RequestBody PushSubscribeDTO body,
            @RequestHeader(value = "User-Id", required = false) String userId,
            @RequestHeader("User-Role") String role) {
        checkAdmin(role);
        try {
            boolean created = subscriptionRepository.findByEndpoint(body.endpoint())
                    .map(existing -> {
                        existing.setP256dh(body.keys().p256dh());
                        existing.setAuth(body.keys().auth());
                        existing.setUserId(userId);
                        subscriptionRepository.save(existing);
                        return false;
                    })
                    .orElseGet(() -> {
                        PushSubscription sub = new PushSubscription();
                        sub.setEndpoint(body.endpoint());
                        sub.setP256dh(body.keys().p256dh());
                        sub.setAuth(body.keys().auth());
                        sub.setUserId(userId);
                        sub.setCreatedAt(Instant.now());
                        subscriptionRepository.save(sub);
                        return true;
                    });
            return created
                    ? ResponseEntity.status(HttpStatus.CREATED).build()
                    : ResponseEntity.ok().build();
        } catch (DuplicateKeyException e) {
            subscriptionRepository.findByEndpoint(body.endpoint()).ifPresent(existing -> {
                existing.setP256dh(body.keys().p256dh());
                existing.setAuth(body.keys().auth());
                existing.setUserId(userId);
                subscriptionRepository.save(existing);
            });
            return ResponseEntity.ok().build();
        }
    }

    @DeleteMapping("/subscriptions")
    public ResponseEntity<Void> unsubscribe(
            @RequestParam String endpoint,
            @RequestHeader("User-Role") String role) {
        checkAdmin(role);
        subscriptionRepository.deleteByEndpoint(endpoint);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/test")
    public Map<String, Object> test(@RequestHeader("User-Role") String role) {
        checkAdmin(role);
        long count = subscriptionRepository.count();
        pushNotificationService.sendToAdmins(
                "Notificación de prueba",
                "Si ves esto, el push remoto funciona.",
                frontendUrl);
        return Map.of("subscriptions", count);
    }

    private void checkAdmin(String role) {
        if (!"ROLE_ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso prohíbido");
        }
    }
}
