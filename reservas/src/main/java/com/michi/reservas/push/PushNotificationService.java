package com.michi.reservas.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.michi.reservas.model.PushSubscription;
import com.michi.reservas.repository.PushSubscriptionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Security;
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
        for (PushSubscription sub : subscriptions) {
            try {
                Notification notification = new Notification(
                        sub.getEndpoint(), sub.getP256dh(), sub.getAuth(), bytes);
                HttpResponse response = sender.send(notification);
                int status = response.getStatusLine().getStatusCode();
                if (status == 404 || status == 410) {
                    subscriptionRepository.delete(sub);
                    log.info("Suscripción push caducada ({}), eliminada", status);
                } else if (status >= 400) {
                    log.warn("Push rechazado con status {} para {}", status, sub.getEndpoint());
                }
            } catch (Exception e) {
                log.error("Error enviando push a {}", sub.getEndpoint(), e);
            }
        }
    }
}
