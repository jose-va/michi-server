package com.michi.gateway.configuration;

import com.michi.gateway.service.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JwtCookieFilterTests {

    private static final String SECRET = "0123456789-0123456789-0123456789-01";

    private WebFilter filter;

    @BeforeEach
    void setUp() {
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        filter = new SecurityConfig(jwtService).jwtCookieFilter();
    }

    private HttpHeaders run(MockServerHttpRequest.BaseBuilder<?> request) {
        AtomicReference<HttpHeaders> seen = new AtomicReference<>();
        WebFilterChain chain = (ServerWebExchange exchange) -> {
            seen.set(exchange.getRequest().getHeaders());
            return Mono.empty();
        };
        filter.filter(MockServerWebExchange.from(request), chain).block();
        return seen.get();
    }

    private String token(String secret, Date expiration) {
        return Jwts.builder()
                .subject("123")
                .issuer("michi-oauth")
                .claim("role", "ROLE_USER")
                .claim("name", "Michi")
                .claim("email", "michi@test.com")
                .expiration(expiration)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private Date inOneHour() {
        return new Date(System.currentTimeMillis() + 3_600_000L);
    }

    @Test
    void withoutCookieForgedHeadersAreRemoved() {
        HttpHeaders headers = run(MockServerHttpRequest.get("/auth/user")
                .header("User-Role", "ROLE_ADMIN")
                .header("User-Id", "999"));

        assertNull(headers.getFirst("User-Role"));
        assertNull(headers.getFirst("User-Id"));
    }

    @Test
    void invalidTokenRemovesForgedHeaders() {
        HttpHeaders headers = run(MockServerHttpRequest.get("/auth/user")
                .cookie(new org.springframework.http.HttpCookie("token", "no-es-un-jwt"))
                .header("User-Role", "ROLE_ADMIN"));

        assertNull(headers.getFirst("User-Role"));
    }

    @Test
    void tokenSignedWithOtherSecretIsRejected() {
        String forged = token("otro-secreto-otro-secreto-otro-secreto-1", inOneHour());

        HttpHeaders headers = run(MockServerHttpRequest.get("/auth/user")
                .cookie(new org.springframework.http.HttpCookie("token", forged))
                .header("User-Role", "ROLE_ADMIN"));

        assertNull(headers.getFirst("User-Role"));
        assertNull(headers.getFirst("User-Id"));
    }

    @Test
    void expiredTokenIsRejected() {
        String expired = token(SECRET, new Date(System.currentTimeMillis() - 1000L));

        HttpHeaders headers = run(MockServerHttpRequest.get("/auth/user")
                .cookie(new org.springframework.http.HttpCookie("token", expired)));

        assertNull(headers.getFirst("User-Id"));
    }

    @Test
    void validTokenReplacesForgedHeaders() {
        HttpHeaders headers = run(MockServerHttpRequest.get("/auth/user")
                .cookie(new org.springframework.http.HttpCookie("token", token(SECRET, inOneHour())))
                .header("User-Role", "ROLE_ADMIN")
                .header("User-Id", "999"));

        assertEquals("123", headers.getFirst("User-Id"));
        assertEquals("ROLE_USER", headers.getFirst("User-Role"));
        assertEquals(1, headers.get("User-Role").size());
        assertEquals("Michi", headers.getFirst("User-Name"));
        assertEquals("michi@test.com", headers.getFirst("User-Email"));
    }

    @Test
    void validTokenWithoutOptionalClaimsUsesDefaults() {
        HttpHeaders headers = run(MockServerHttpRequest.get("/auth/user")
                .cookie(new org.springframework.http.HttpCookie("token", token(SECRET, inOneHour()))));

        assertEquals("", headers.getFirst("User-Picture"));
    }
}
