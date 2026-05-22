package com.michi.oauth.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    public String generateToken(OAuth2User user) {
        String role = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_USER");

        return Jwts.builder()
                .subject(user.getAttribute("email"))
                .claim("role", role)
                .claim("name", user.getAttribute("name"))
                .claim("picture", user.getAttribute("picture"))
                .claim("googleId", user.getAttribute("sub"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 604800000L))
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes()))
                .compact();
    }
}
