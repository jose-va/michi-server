package com.michi.reservas.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.michi.reservas.model.PushSubscription;
import com.michi.reservas.repository.PushSubscriptionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private final PushSubscriptionRepository subscriptionRepository;
    private final PushProperties pushProperties;
    private final ObjectMapper objectMapper;

    private volatile PushService pushService;

    @PostConstruct
    void init() {
        Security.addProvider(new BouncyCastleProvider());
        if (pushProperties.publicKey() == null || pushProperties.publicKey().isBlank()
                || pushProperties.privateKey() == null || pushProperties.privateKey().isBlank()) {
            log.warn("VAPID keys no configuradas (VAPID_PUBLIC_KEY/VAPID_PRIVATE_KEY), push desactivado");
            return;
        }
        try {
            pushService = new PushService(
                    pushProperties.publicKey(), pushProperties.privateKey(), pushProperties.subject());
            log.info("VAPID inicializado pub={} subject={}",
                    vapidPrefix(pushProperties.publicKey()), pushProperties.subject());
        } catch (Exception e) {
            log.error("No se pudo inicializar el servicio de push", e);
        }
    }

    public void sendToAdmins(String title, String body, String url) {
        PushService sender = pushService;
        if (sender == null) {
            log.warn("Push no configurado, no se envía: {}", title);
            return;
        }
        List<PushSubscription> subscriptions = subscriptionRepository.findAll();
        if (subscriptions.isEmpty()) {
            log.info("Sin suscripciones push, no se envía: {}", title);
            return;
        }
        String payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of(
                    "title", title == null ? "" : title,
                    "body", body == null ? "" : body,
                    "url", url == null ? "" : url));
        } catch (Exception e) {
            log.error("No se pudo serializar el payload push", e);
            return;
        }
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        log.info("Enviando push a {} subs con VAPID pub={}", subscriptions.size(),
                vapidPrefix(pushProperties.publicKey()));
        for (PushSubscription sub : subscriptions) {
            try {
                Notification notification = new Notification(
                        sub.getEndpoint(), sub.getP256dh(), sub.getAuth(), bytes);
                // web-push 5.1.2 construye Crypto-Key p256ecdsa en base64url (-_,
                // con pad) y FCM lo rechaza con 403: exige base64 estándar (+/,
                // con pad). Se reutiliza preparePost de la lib (cifrado + JWT
                // VAPID intactos) y solo se normaliza ese header antes de enviar.
                HttpPost post = sender.preparePost(notification, Encoding.AES128GCM);
                normalizeCryptoKeyHeader(post);
                HttpResponse response;
                try (CloseableHttpClient http = HttpClients.createDefault()) {
                    response = http.execute(post);
                }
                int status = response.getStatusLine().getStatusCode();
                if (status == 404 || status == 410) {
                    subscriptionRepository.delete(sub);
                    log.info("Suscripción push caducada ({}), eliminada", status);
                } else if (status >= 400) {
                    log.warn("Push rechazado con status {} para {} vapidPub={} fcmBody={}",
                            status, sub.getEndpoint(),
                            vapidPrefix(pushProperties.publicKey()), responseBody(response));
                }
            } catch (Exception e) {
                log.error("Error enviando push a {}", sub.getEndpoint(), e);
            }
        }
    }

    private static String vapidPrefix(String publicKey) {
        if (publicKey == null || publicKey.length() <= 8) {
            return String.valueOf(publicKey);
        }
        return publicKey.substring(0, 8) + "... len=" + publicKey.length();
    }

    private static String responseBody(HttpResponse response) {
        try {
            if (response.getEntity() == null) {
                return "<sin body>";
            }
            return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "<body ilegible: " + e + ">";
        }
    }

    /**
     * FCM exige {@code p256ecdsa} en base64 estándar (+/, con pad), pero
     * web-push 5.1.2 lo emite en base64url (-_, con pad). Decodifica el valor
     * tal cual venga (url o estándar) y lo re-emite en estándar con pad.
     * Solo toca ese segmento; el resto del header queda intacto.
     */
    static void normalizeCryptoKeyHeader(HttpPost post) {
        Header header = post.getFirstHeader("Crypto-Key");
        if (header == null) {
            return;
        }
        List<String> out = new ArrayList<>();
        boolean changed = false;
        for (String part : header.getValue().split(";")) {
            String token = part.trim();
            if (token.startsWith("p256ecdsa=")) {
                String encoded = token.substring("p256ecdsa=".length()).trim();
                byte[] raw = decodeBase64UrlOrStd(encoded);
                String std = Base64.getEncoder().encodeToString(raw);
                if (!std.equals(encoded)) {
                    changed = true;
                }
                out.add("p256ecdsa=" + std);
            } else {
                out.add(token);
            }
        }
        if (changed) {
            post.setHeader("Crypto-Key", String.join(";", out));
            log.debug("Crypto-Key normalizado a base64 estándar");
        }
    }

    private static byte[] decodeBase64UrlOrStd(String value) {
        try {
            return Base64.getUrlDecoder().decode(value);
        } catch (IllegalArgumentException urlFail) {
            return Base64.getDecoder().decode(value);
        }
    }
}
