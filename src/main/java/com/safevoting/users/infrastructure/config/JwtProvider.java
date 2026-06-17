package com.safevoting.users.infrastructure.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtProvider implements com.safevoting.users.domain.repository.TokenService {

    private final SecretKey secretKey;
    private final long expirationMinutes;

    public JwtProvider(@Value("${jwt.secret}") String secret,
                       @Value("${jwt.expiration-minutes}") long expirationMinutes) {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMinutes = expirationMinutes;
    }

    @Override
    public String generarToken(UUID usuarioId, String email, String rol) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + expirationMinutes * 60 * 1000);

        return Jwts.builder()
                .subject(email)
                .claims(Map.of("rol", rol, "uid", usuarioId.toString()))
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(secretKey)
                .compact();
    }

    public Claims validarToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extraerEmail(String token) {
        return validarToken(token).getSubject();
    }

    public String extraerRol(String token) {
        return validarToken(token).get("rol", String.class);
    }

    public UUID extraerUsuarioId(String token) {
        String uid = validarToken(token).get("uid", String.class);
        return UUID.fromString(uid);
    }
}
