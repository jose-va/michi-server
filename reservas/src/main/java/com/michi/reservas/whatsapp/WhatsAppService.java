package com.michi.reservas.whatsapp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class WhatsAppService {

    private final WebClient client;
    private final String phoneNumberId;
    private final String templateName;
    private final String language;
    private final String headerImageUrl;

    public WhatsAppService(WebClient whatsappClient,
                           @Value("${whatsapp.phone-number-id}") String phoneNumberId,
                           @Value("${whatsapp.template-name}") String templateName,
                           @Value("${whatsapp.language}") String language,
                           @Value("${whatsapp.header-image-url}") String headerImageUrl) {
        this.client = whatsappClient;
        this.phoneNumberId = phoneNumberId;
        this.templateName = templateName;
        this.language = language;
        this.headerImageUrl = headerImageUrl;
    }

    public void sendConfirmation(String telefono, String nombre, String fecha, String hora) {
        Map<String, Object> header = Map.of(
                "type", "header",
                "parameters", List.of(Map.of(
                        "type", "image",
                        "image", Map.of("link", headerImageUrl)
                ))
        );

        Map<String, Object> bodyComponent = Map.of(
                "type", "body",
                "parameters", List.of(
                        Map.of("type", "text", "text", nombre),
                        Map.of("type", "text", "text", fecha),
                        Map.of("type", "text", "text", hora)
                )
        );

        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", telefono,
                "type", "template",
                "template", Map.of(
                        "name", templateName,
                        "language", Map.of("code", language)
//                        "components", List.of(header, bodyComponent)
                )
        );

        client.post()
                .uri("/{id}/messages", phoneNumberId)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(e -> e instanceof WebClientResponseException w
                                && w.getStatusCode().is5xxServerError()))
                .doOnSuccess(r -> log.info("WhatsApp enviado a {}: {}", telefono, r))
                .doOnError(e -> {
                    if (e instanceof WebClientResponseException w) {
                        log.error("Error WhatsApp {} a {}: {}", w.getStatusCode(),
                                telefono, w.getResponseBodyAsString());
                    } else {
                        log.error("Error enviando WhatsApp a {}", telefono, e);
                    }
                })
                .subscribe(r -> {}, e -> {});
    }
}