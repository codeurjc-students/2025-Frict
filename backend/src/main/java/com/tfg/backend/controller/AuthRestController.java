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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@RestController
// @Slf4j // For custom logs (log.warn("Warning message"))
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication Management", description = "Users authentication management")
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


    @Operation(summary = "(User) Login with Google account")
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
            throw new BadCredentialsException("Error while verifying Google token.", e);
        }

        if (idToken == null) {
            throw new BadCredentialsException("Invalid or expired Google token.");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();

        if(userService.isBannedByEmail(email)){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "This user is banned.");
        }

        if(userService.isDeletedByEmail(email)){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This user has been previously deleted.");
        }


        boolean isNewUser = !userService.existsByEmail(email);
        AuthResponse authResponse = loginService.loginWithGoogle(response, payload);

        // If new user, then build location header
        if (isNewUser) {
            User user = userService.findByEmail(email).orElseThrow();
            URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/v1/users/{id}")
                    .buildAndExpand(user.getId())
                    .toUri();
            return ResponseEntity.created(location).body(authResponse);
        }

        // If user already existed, then return 200
        return ResponseEntity.ok(authResponse);
    }


    @Operation(summary = "(All) Login with local account")
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
    @Operation(summary = "(User) Create user account")
    @PostMapping("/signup")
    public ResponseEntity<UserLoginDTO> registerUser(@RequestBody UserSignupDTO registerDTO) {
        if (userService.isUsernameTaken(registerDTO.getUsername()) || userService.isEmailTaken(registerDTO.getEmail())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "This username or email is already taken.");
        }

        User newUser = userService.registerUser(registerDTO);

        if (newUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Internal error during user signup.");
        }

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(newUser.getId())
                .toUri();

        return ResponseEntity.created(location).body(new UserLoginDTO(newUser));
    }


    @Operation(summary = "(All) Refresh logged user JWT access token")
	@PostMapping("/refresh")
	public ResponseEntity<AuthResponse> refreshToken(
			@CookieValue(name = "RefreshToken", required = false) String refreshToken, HttpServletResponse response) {

		return loginService.refresh(response, refreshToken);
	}


    @Operation(summary = "(All) Log out logged user")
	@PostMapping("/logout")
	public ResponseEntity<AuthResponse> logOut(HttpServletResponse response) {
		return ResponseEntity.ok(new AuthResponse(Status.SUCCESS, loginService.logout(response)));
	}


    @Operation(summary = "(All) Recover user account")
    @PostMapping("/recovery")
    public ResponseEntity<Void> recoverPassword(@RequestBody Map<String, String> payload) {
        User user = userService.findUserHelper(payload.get("username"));

        SecureRandom secureRandom = new SecureRandom();
        String newOtp = String.format("%06d", secureRandom.nextInt(1000000)); //6-digit OTP
        user.setOtpCode(newOtp);
        user.setOtpExpiration(LocalDateTime.now().plusMinutes(15));
        User savedUser = userService.save(user);

        emailService.sendRecoveryOtp(savedUser.getEmail(), savedUser.getUsername(), savedUser.getOtpCode());
        return ResponseEntity.ok().build();
    }


    @Operation(summary = "(All) Verify user OTP")
    @PostMapping("/verification")
    public ResponseEntity<Boolean> verifyOtp(@RequestBody Map<String, String> payload) {
        User user = userService.findUserHelper(payload.get("username"));

        if (!user.isOtpValid(payload.get("otpCode"))){
            return ResponseEntity.ok(false);
        }
        user.setOtpCode(null);
        user.setOtpExpiration(null);
        return ResponseEntity.ok(true);
    }


    @Operation(summary = "(All) Reset user password")
    @PutMapping("/reset")
    public ResponseEntity<Void> resetPassword(@RequestBody Map<String, String> payload) {
        User user = userService.findUserHelper(payload.get("username"));

        if (!user.isOtpValid(payload.get("otpCode"))){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "OTP code does not match or is expired.");
        }

        user.setEncodedPassword(passwordEncoder.encode(payload.get("newPassword")));
        userService.save(user);
        return ResponseEntity.ok().build();
    }


    @Operation(summary = "(Admin) Reset an administration account password")
    @PutMapping("/reset/{id}")
    public ResponseEntity<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String password = payload.get("password");
        String repeatPassword = payload.get("repeatPassword");

        if (password == null || repeatPassword == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missed required fields.");
        }

        if (!password.equals(repeatPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match.");
        }

        Optional<User> userOptional = userService.findById(id);

        if (userOptional.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User with ID " + id + " does not exist.");
        }
        User user = userOptional.get();

        if (user.getRoles().contains("USER")){
            throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "User password cannot be changed by other accounts.");
        }

        user.setEncodedPassword(passwordEncoder.encode(password));
        userService.save(user);
        return ResponseEntity.ok().build();
    }
}