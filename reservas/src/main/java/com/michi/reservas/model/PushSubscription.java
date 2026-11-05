package com.michi.reservas.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("push_subscriptions")
@Data
public class PushSubscription {
    @Id
    private String id;

    @Indexed(unique = true)
    private String endpoint;

    private String p256dh;
    private String auth;
    private String userId;
    private Instant createdAt;
}
