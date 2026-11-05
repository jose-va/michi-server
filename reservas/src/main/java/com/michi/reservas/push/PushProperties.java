package com.michi.reservas.push;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "push.vapid")
public record PushProperties(String publicKey, String privateKey, String subject) {
}
