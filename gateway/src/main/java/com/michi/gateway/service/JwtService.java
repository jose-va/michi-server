package com.michi.gateway.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)))
                .requireIssuer("michi-oauth")
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}