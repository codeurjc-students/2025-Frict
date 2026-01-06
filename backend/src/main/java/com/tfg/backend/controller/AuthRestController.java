package com.tfg.backend.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.tfg.backend.dto.UserLoginDTO;
import com.tfg.backend.dto.UserSignupDTO;
import com.tfg.backend.model.User;
import com.tfg.backend.security.GoogleTokenDTO;
import com.tfg.backend.security.jwt.AuthResponse;
import com.tfg.backend.security.jwt.AuthResponse.Status;
import com.tfg.backend.security.jwt.LoginRequest;
import com.tfg.backend.security.jwt.UserLoginService;
import com.tfg.backend.service.UserService;
import com.tfg.backend.utils.EmailService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@RestController
// @Slf4j // For custom logs (log.warn("Warning message"))
@RequestMapping("/api/v1/auth")
public class AuthRestController {
	
	@Autowired
	private UserLoginService loginService;

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${google.auth.clientId}")
    private String googleClientId;

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginWithGoogle(HttpServletResponse response,
                                             @RequestBody GoogleTokenDTO tokenDTO) {
        GoogleIdToken idToken;
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            idToken = verifier.verify(tokenDTO.token());
        } catch (Exception e) {
            throw new BadCredentialsException("Error verificando token Google.", e);
        }

        if (idToken == null) {
            throw new BadCredentialsException("Token de Google inválido o expirado.");
        }

        // Pasamos el Payload al servicio, él sabrá qué hacer (Login o Registro)
        GoogleIdToken.Payload payload = idToken.getPayload();
        //Check if the user is banned or deleted
        if(userService.isBannedByEmail(payload.getEmail())){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "This user is banned.");
        }

        if(userService.isDeletedByEmail(payload.getEmail())){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This user has been previously deleted.");
        }
        return ResponseEntity.ok(loginService.loginWithGoogle(response, payload));
    }

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(
			@RequestBody LoginRequest loginRequest,
			HttpServletResponse response) {

        //Check if the user is banned or deleted
        if(userService.isBannedByUsername(loginRequest.getUsername())){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "This user is banned.");
        }

        if(userService.isDeletedByUsername(loginRequest.getUsername())){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This user has been previously deleted.");
        }
		
		return ResponseEntity.ok(loginService.login(response, loginRequest));
	}

    //Spring automatically matches the form fields with the same name and generates an UserSignupDTO object
    @PostMapping("/signup")
    public ResponseEntity<UserLoginDTO> registerUser(@RequestBody UserSignupDTO registerDTO) {
        if (userService.isUsernameTaken(registerDTO.getUsername()) || userService.isEmailTaken(registerDTO.getEmail())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "This username or email is already taken.");
        }

        User newUser = userService.registerUser(registerDTO);

        if (newUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Internal error during user signup.");
        }

        return ResponseEntity.ok(new UserLoginDTO(newUser));
    }

	@PostMapping("/refresh")
	public ResponseEntity<AuthResponse> refreshToken(
			@CookieValue(name = "RefreshToken", required = false) String refreshToken, HttpServletResponse response) {

		return loginService.refresh(response, refreshToken);
	}

	@PostMapping("/logout")
	public ResponseEntity<AuthResponse> logOut(HttpServletResponse response) {
		return ResponseEntity.ok(new AuthResponse(Status.SUCCESS, loginService.logout(response)));
	}

    @PostMapping("/recovery")
    public ResponseEntity<Void> recoverPassword(@RequestBody Map<String, String> payload) {
        User user = findUserHelper(payload.get("username"));

        SecureRandom secureRandom = new SecureRandom();
        String newOtp = String.format("%06d", secureRandom.nextInt(1000000)); //6-digit OTP
        user.setOtpCode(newOtp);
        user.setOtpExpiration(LocalDateTime.now().plusMinutes(15));
        User savedUser = userService.save(user);

        emailService.sendRecoveryOtp(savedUser.getEmail(), savedUser.getUsername(), savedUser.getOtpCode());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verification")
    public ResponseEntity<Boolean> verifyOtp(@RequestBody Map<String, String> payload) {
        User user = findUserHelper(payload.get("username"));

        if (!user.isOtpValid(payload.get("otpCode"))){
            return ResponseEntity.ok(false);
        }
        user.setOtpCode(null);
        user.setOtpExpiration(null);
        return ResponseEntity.ok(true);
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> resetPassword(@RequestBody Map<String, String> payload) {
        User user = findUserHelper(payload.get("username"));

        if (!user.isOtpValid(payload.get("otpCode"))){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "OTP code does not match or is expired.");
        }

        user.setEncodedPassword(passwordEncoder.encode(payload.get("newPassword")));
        userService.save(user);
        return ResponseEntity.ok().build();
    }

    private User findUserHelper(String username) {
        return this.userService.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "The user '" + username + "' does not exist.")); //Captured by ResponseStatusExceptionResolver (Spring DispatcherServlet internal helper class)
    }
}