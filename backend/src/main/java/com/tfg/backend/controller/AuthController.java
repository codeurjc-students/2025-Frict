package com.tfg.backend.controller;

import java.util.*;

import com.tfg.backend.DTO.UserLoginDTO;
import com.tfg.backend.DTO.UserRegisterDTO;
import com.tfg.backend.model.User;
import com.tfg.backend.security.jwt.JwtTokenProvider;
import com.tfg.backend.security.jwt.Token;
import com.tfg.backend.security.jwt.UserLoginService;
import com.tfg.backend.service.AuthService;
import com.tfg.backend.service.TokenService;
import com.tfg.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private AuthService authService;
    @Autowired
    private UserService userService;
    @Autowired
    private TokenService tokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserLoginService userLoginService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserLoginDTO loginDTO) {
        // Autenticar al usuario utilizando el servicio
        Optional<Map<String, Object>> authResult = authService.authenticate(loginDTO);

        if (authResult.isPresent()) {
            Map<String, Object> response = authResult.get();
            String token = (String) response.get("token");

            UserDetails user = (UserDetails) response.get("user");

            // Generar los nuevos tokens
            Token newAccessToken = jwtTokenProvider.generateToken(user);
            Token newRefreshToken = jwtTokenProvider.generateRefreshToken(user);

            // Crear las cabeceras para agregar las cookies
            HttpHeaders responseHeaders = new HttpHeaders();
            userLoginService.addAccessTokenCookie(responseHeaders, newAccessToken);  // Usamos UserLoginService
            userLoginService.addRefreshTokenCookie(responseHeaders, newRefreshToken);  // Usamos UserLoginService

            // Devolver la respuesta con los tokens en las cookies
            return ResponseEntity.ok().headers(responseHeaders).body(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("error", "Backend: Invalid credentials or banned user"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerUser(@RequestBody UserRegisterDTO registerDTO) {
        if (authService.isEmailTaken(registerDTO.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("error", "Backend: Email in use"));
        }

        User newUser = authService.registerUser(registerDTO);

        if (newUser == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Backend: Error signing up"));
        }

        String token = tokenService.generateToken(newUser);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Backend: User signed up successfully");
        response.put("role", newUser.getRole());
        response.put("token", token);

        return ResponseEntity.ok(response);
    }


    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader("Authorization") String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("error", "Token not provided"));
        }

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);  // Elimina el prefijo "Bearer " del token si está presente
        }

        // Invalidar el token
        boolean isLoggedOut = tokenService.invalidateToken(token);

        // Responder dependiendo de si la invalidación fue exitosa
        if (isLoggedOut) {
            return ResponseEntity.ok(Collections.singletonMap("message", "Logout completed successfully"));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("error", "Not valid or expired token"));
        }
    }



}


