package com.tfg.backend.controller;

import com.tfg.backend.security.jwt.AuthResponse;
import com.tfg.backend.security.jwt.LoginRequest;
import com.tfg.backend.security.jwt.AuthService;
import com.tfg.backend.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthRestController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {

        return authService.login(response, loginRequest);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@CookieValue(name = "RefreshToken", required = false) String refreshToken, HttpServletResponse response) {

        return authService.refresh(response, refreshToken);
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logOut(HttpServletResponse response) {
        return ResponseEntity.ok(new AuthResponse(AuthResponse.Status.SUCCESS, authService.logout(response)));
    }

}


