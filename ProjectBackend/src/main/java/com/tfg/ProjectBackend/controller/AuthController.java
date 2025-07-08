package com.tfg.ProjectBackend.controller;

import java.util.*;

import com.tfg.ProjectBackend.DTO.UserLoginDTO;
import com.tfg.ProjectBackend.DTO.UserRegisterDTO;
import com.tfg.ProjectBackend.model.User;
import com.tfg.ProjectBackend.security.jwt.JwtTokenProvider;
import com.tfg.ProjectBackend.security.jwt.Token;
import com.tfg.ProjectBackend.security.jwt.UserLoginService;
import com.tfg.ProjectBackend.service.AuthService;
import com.tfg.ProjectBackend.service.TokenService;
import com.tfg.ProjectBackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
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

            // Generamos los nuevos tokens
            Token newAccessToken = jwtTokenProvider.generateToken(user);
            Token newRefreshToken = jwtTokenProvider.generateRefreshToken(user);

            // Creamos las cabeceras para agregar las cookies
            HttpHeaders responseHeaders = new HttpHeaders();
            userLoginService.addAccessTokenCookie(responseHeaders, newAccessToken);  // Usamos UserLoginService
            userLoginService.addRefreshTokenCookie(responseHeaders, newRefreshToken);  // Usamos UserLoginService

            // Devolvemos la respuesta con los tokens en las cookies
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
        // Verificar si el token está presente y no es vacío
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("error", "Token not provided"));
        }

        // Eliminar el prefijo "Bearer " del token si está presente
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);  // Elimina el prefijo "Bearer "
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


