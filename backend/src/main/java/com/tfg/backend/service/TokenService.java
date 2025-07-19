package com.tfg.backend.service;

import com.tfg.backend.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenService {

    @Autowired
    private UserService userService;

    @Value("${jwt.secret}")
    private String secretKey;

    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 6; // 6 horas
    private final Set<String> invalidatedTokens = ConcurrentHashMap.newKeySet();


    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("role", user.getRole())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody();
        return claims.getSubject();
    }
    public boolean invalidateToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        invalidatedTokens.add(token);
        return true;
    }

    public Optional<User> getUserFromToken(String token) {
        String userEmail = decodeToken(token);
        Optional<User> userOptional = userService.findByEmail(userEmail);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            if (user.isBanned()) {
                return Optional.empty();
            }

            return Optional.of(user);
        }
        return Optional.empty();
    }

    private String decodeToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secretKey)
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject(); // Email del usuario en subject del token
        } catch (Exception e) {
            return null; // Para errores de token inválido
        }
    }
}

