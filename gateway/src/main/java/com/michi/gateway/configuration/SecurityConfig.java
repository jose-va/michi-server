package com.michi.gateway.configuration;

import com.michi.gateway.service.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.WebFilter;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private static final List<String> USER_HEADERS =
            List.of("User-Id", "User-Role", "User-Name", "User-Picture", "User-Email");

    private final JwtService jwtService;

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        return http
                .cors(Customizer.withDefaults())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(auth -> auth
                        .pathMatchers(HttpMethod.GET, "/oauth2/**", "/login/oauth2/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/auth/logout").permitAll()
                        .pathMatchers(HttpMethod.GET, "/auth/user").authenticated()
                        .pathMatchers(HttpMethod.GET, "/auth/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/products", "/products/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/reservations/hours").permitAll()
                        .pathMatchers(HttpMethod.POST, "/reservations").permitAll()
                        .pathMatchers(HttpMethod.DELETE, "/reservations/*").permitAll()
                        .anyExchange().authenticated()
                )
                .addFilterBefore(jwtCookieFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    @Bean
    public WebFilter jwtCookieFilter() {
        return (exchange, chain) -> {
            ServerHttpRequest sanitized = exchange.getRequest().mutate()
                    .headers(h -> USER_HEADERS.forEach(h::remove))
                    .build();
            ServerWebExchange cleanExchange = exchange.mutate().request(sanitized).build();

            HttpCookie cookie = sanitized.getCookies().getFirst("token");

            if (cookie == null) return chain.filter(cleanExchange);

            try {
                Claims claims = jwtService.validateToken(cookie.getValue());

                String subject = claims.getSubject();
                String role = claim(claims, "role", "ROLE_USER");

                ServerHttpRequest mutated = sanitized.mutate()
                        .headers(h -> {
                            h.set("User-Id", subject == null ? "" : subject);
                            h.set("User-Role", role);
                            h.set("User-Name", claim(claims, "name", ""));
                            h.set("User-Picture", claim(claims, "picture", ""));
                            h.set("User-Email", claim(claims, "email", ""));
                        })
                        .build();

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                subject, null, List.of(new SimpleGrantedAuthority(role))
                        );

                return chain.filter(cleanExchange.mutate().request(mutated).build())
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

            } catch (Exception e) {
                log.warn("Token JWT rechazado: {}", e.getMessage());
                return chain.filter(cleanExchange);
            }
        };
    }

    private static String claim(Claims claims, String name, String fallback) {
        String value = claims.get(name, String.class);
        return value == null ? fallback : value;
    }
}
