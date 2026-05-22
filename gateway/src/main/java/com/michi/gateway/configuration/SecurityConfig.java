package com.michi.gateway.configuration;

import com.michi.gateway.service.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
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
public class SecurityConfig {

    private final JwtService jwtService;

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        return http
                .cors(Customizer.withDefaults())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(auth -> auth
                        .pathMatchers(HttpMethod.GET, "/auth").permitAll()
                        .pathMatchers(HttpMethod.GET, "/products", "/products/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/reservations/hours").permitAll()
                        .pathMatchers(HttpMethod.POST, "/reservations").permitAll()
                        .anyExchange().authenticated()
                )
                .addFilterBefore(jwtCookieFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    @Bean
    public WebFilter jwtCookieFilter() {
        return (exchange, chain) -> {
            HttpCookie cookie = exchange.getRequest().getCookies().getFirst("token");

            if (cookie == null) return chain.filter(exchange);

            try {
                Claims claims = jwtService.validateToken(cookie.getValue());

                ServerHttpRequest mutated = exchange.getRequest().mutate()
                        .header("User-Email",    claims.getSubject())
                        .header("User-Role",  claims.get("role", String.class))
                        .header("User-Name",  claims.get("name", String.class))
                        .header("User-Picture",  claims.get("picture", String.class))
                        .header("User-Id",  claims.get("googleId", String.class))
                        .build();

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                claims.getSubject(), null, List.of(
                                new SimpleGrantedAuthority(claims.get("role", String.class))
                        )
                        );

                return chain.filter(exchange.mutate().request(mutated).build())
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

            } catch (Exception e) {
                return chain.filter(exchange);
            }
        };
    }
}
