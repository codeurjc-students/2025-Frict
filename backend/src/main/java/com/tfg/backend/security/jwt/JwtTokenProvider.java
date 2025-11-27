package com.tfg.backend.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    // Secret string from application.properties. Must be a fixed text. If not, cookies stored in browser will have a key that will never match with the key expected by the server (which will be randomly generated each time)
    @Value("${jwt.secret}")
    private String secretString;

    // Helper method to convert the Base64 string into a SecretKey
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretString);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Update validateToken to use the fixed key
    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String tokenStringFromHeaders(HttpServletRequest req){
        String bearerToken = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (bearerToken == null) {
            throw new IllegalArgumentException("Missing Authorization header");
        }
        if(!bearerToken.startsWith("Bearer ")){
            throw new IllegalArgumentException("Authorization header does not start with Bearer: " + bearerToken);
        }
        return bearerToken.substring(7);
    }

    private String tokenStringFromCookies(HttpServletRequest request) {
        var cookies = request.getCookies();
        if (cookies == null) {
            throw new IllegalArgumentException("No cookies found in request");
        }

        for (Cookie cookie : cookies) {
            if (TokenType.ACCESS.cookieName.equals(cookie.getName())) {
                String accessToken = cookie.getValue();
                if (accessToken == null) {
                    throw new IllegalArgumentException("Cookie %s has null value".formatted(TokenType.ACCESS.cookieName));
                }

                return accessToken;
            }
        }
        throw new IllegalArgumentException("No access token cookie found in request");
    }

    public Claims validateToken(HttpServletRequest req, boolean fromCookie){
        var token = fromCookie?
                tokenStringFromCookies(req):
                tokenStringFromHeaders(req);
        return validateToken(token);
    }

    public String generateAccessToken(UserDetails userDetails) {
        return buildToken(TokenType.ACCESS, userDetails).compact();
    }

    public String generateRefreshToken(UserDetails userDetails) {
        var token = buildToken(TokenType.REFRESH, userDetails);
        return token.compact();
    }

    private JwtBuilder buildToken(TokenType tokenType, UserDetails userDetails) {
        var currentDate = new Date();
        var expiryDate = Date.from(new Date().toInstant().plus(tokenType.duration));
        return Jwts.builder()
                .claim("roles", userDetails.getAuthorities())
                .claim("type", tokenType.name())
                .subject(userDetails.getUsername())
                .issuedAt(currentDate)
                .expiration(expiryDate)
                .signWith(getSignInKey()); // Use the fixed key for signing
    }
}