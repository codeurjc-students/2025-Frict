package com.tfg.backend.controller;

import com.tfg.backend.DTO.UserLoginDTO;
import com.tfg.backend.DTO.UserSignupDTO;
import com.tfg.backend.model.User;
import com.tfg.backend.security.jwt.AuthResponse;
import com.tfg.backend.security.jwt.AuthResponse.Status;
import com.tfg.backend.security.jwt.LoginRequest;
import com.tfg.backend.security.jwt.UserLoginService;
import com.tfg.backend.service.UserService;
import com.tfg.backend.utils.EmailService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
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

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(
			@RequestBody LoginRequest loginRequest,
			HttpServletResponse response) {
		
		return loginService.login(response, loginRequest);
	}

    //Spring automatically matches the form fields with the same name and generates an UserSignupDTO object
    @PostMapping("/signup")
    public ResponseEntity<UserLoginDTO> registerUser(@RequestBody UserSignupDTO registerDTO) {
        if (userService.isUsernameTaken(registerDTO.getUsername()) || userService.isEmailTaken(registerDTO.getEmail())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User newUser = userService.registerUser(registerDTO);

        if (newUser == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
        Optional<User> userOptional = this.userService.findByUsername(payload.get("username"));
        if(userOptional.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        User user = userOptional.get();

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
        Optional<User> userOptional = this.userService.findByUsername(payload.get("username"));
        if(userOptional.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        User user = userOptional.get();

        if (!user.isOtpValid(payload.get("otpCode"))){
            return ResponseEntity.ok(false);
        }
        return ResponseEntity.ok(true);
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> resetPassword(@RequestBody Map<String, String> payload) {
        Optional<User> userOptional = this.userService.findByUsername(payload.get("username"));
        if(userOptional.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        User user = userOptional.get();

        if (!user.isOtpValid(payload.get("otpCode"))){
            return ResponseEntity.status(401).build(); //Unauthorized, as OTP does not match, or it is expired
        }

        user.setEncodedPassword(passwordEncoder.encode(payload.get("newPassword")));
        user.setOtpCode(null);
        userService.save(user);
        return ResponseEntity.ok().build();
    }
}