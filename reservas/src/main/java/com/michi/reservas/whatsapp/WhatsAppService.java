package com.michi.reservas.whatsapp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class WhatsAppService {

    private final WebClient client;
    private final String fromNumber;
    private final String templateName;
    private final String language;
    private final String headerImageUrl;

    public WhatsAppService(WebClient whatsappClient,
                           @Value("${ycloud.from-number}") String fromNumber,
                           @Value("${whatsapp.template-name}") String templateName,
                           @Value("${whatsapp.language}") String language,
                           @Value("${whatsapp.header-image-url}") String headerImageUrl) {
        this.client = whatsappClient;
        this.fromNumber = fromNumber;
        this.templateName = templateName;
        this.language = language;
        this.headerImageUrl = headerImageUrl;
    }

    public void sendConfirmation(String telefono, String nombre, String fecha, String hora, String externalId) {
        if (fromNumber == null || fromNumber.isBlank()) {
            log.error("YCLOUD_FROM_NUMBER no configurado, no se envía WhatsApp a {}", telefono);
            return;
        }

        List<Map<String, Object>> components = new ArrayList<>();

        if (headerImageUrl != null && !headerImageUrl.isBlank()) {
            components.add(Map.of(
                    "type", "header",
                    "parameters", List.of(Map.of(
                            "type", "image",
                            "image", Map.of("link", headerImageUrl)
                    ))
            ));
        }

        components.add(Map.of(
                "type", "body",
                "parameters", List.of(
                        Map.of("type", "text", "text", nombre),
                        Map.of("type", "text", "text", fecha),
                        Map.of("type", "text", "text", hora)
                )
        ));

        Map<String, Object> payload = new HashMap<>();
        payload.put("from", fromNumber);
        payload.put("to", telefono);
        payload.put("type", "template");
        payload.put("template", Map.of(
                "name", templateName,
                "language", Map.of("code", language, "policy", "deterministic"),
                "components", components
        ));
        if (externalId != null && !externalId.isBlank()) {
            payload.put("externalId", externalId);
        }

        client.post()
                .uri("/whatsapp/messages")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(e -> e instanceof WebClientResponseException w
                                && w.getStatusCode().is5xxServerError()))
                .doOnSuccess(r -> log.info("WhatsApp YCloud aceptado para {} (reserva {}): {}", telefono, externalId, r))
                .doOnError(e -> {
                    if (e instanceof WebClientResponseException w) {
                        log.error("Error YCloud {} a {}: {}", w.getStatusCode(),
                                telefono, w.getResponseBodyAsString());
                    } else {
                        log.error("Error enviando WhatsApp a {}", telefono, e);
                    }
                })
                .subscribe(r -> {}, e -> {});
    }
}
