package com.tfg.backend.controller;

import com.tfg.backend.dto.UserLoginDTO;
import com.tfg.backend.dto.UserSignupDTO;
import com.tfg.backend.model.User;
import com.tfg.backend.security.GoogleTokenDTO;
import com.tfg.backend.security.jwt.AuthResponse;
import com.tfg.backend.security.jwt.LoginRequest;
import com.tfg.backend.security.jwt.UserLoginService;
import com.tfg.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@RestController
// @Slf4j // For custom logs (log.warn("Warning message"))
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication Management", description = "Users authentication management")
public class AuthRestController {
	
	@Autowired
	private UserLoginService loginService;

    @Autowired
    private UserService userService;

    @Operation(summary = "(User) Login with Google account")
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginWithGoogle(HttpServletResponse response,
                                                        @RequestBody GoogleTokenDTO tokenDTO) {

        AuthResponse authResponse = loginService.loginWithGoogle(response, tokenDTO);
        // If new user, then build location header
        if (authResponse.getNewUserEmail() != null){
            User user = userService.findByEmail(authResponse.getNewUserEmail()).orElseThrow();
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
		return ResponseEntity.ok(loginService.login(response, loginRequest));
	}


    //Spring automatically matches the form fields with the same name and generates an UserSignupDTO object
    @Operation(summary = "(User) Create user account")
    @PostMapping("/signup")
    public ResponseEntity<UserLoginDTO> registerUser(@RequestBody UserSignupDTO registerDTO) {
        User newUser = userService.registerUser(registerDTO);
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
		return ResponseEntity.ok(loginService.refresh(response, refreshToken));
	}


    @Operation(summary = "(All) Log out logged user")
	@PostMapping("/logout")
	public ResponseEntity<AuthResponse> logOut(HttpServletResponse response) {
		return ResponseEntity.ok(loginService.logout(response));
	}


    @Operation(summary = "(All) Recover user account")
    @PostMapping("/recovery")
    public ResponseEntity<Void> recoverPassword(@RequestBody Map<String, String> payload) {
        loginService.recoverPassword(payload.get("username"));
        return ResponseEntity.ok().build();
    }


    @Operation(summary = "(All) Verify user OTP")
    @PostMapping("/verification")
    public ResponseEntity<Boolean> verifyOtp(@RequestBody Map<String, String> payload) {
        boolean result = loginService.verifyOtp(payload.get("username"), payload.get("otpCode"));
        return ResponseEntity.ok(result);
    }


    @Operation(summary = "(All) Reset user password")
    @PutMapping("/reset")
    public ResponseEntity<Void> resetPassword(@RequestBody Map<String, String> payload) {
        loginService.resetSelfPassword(payload.get("username"), payload.get("otpCode"), payload.get("newPassword"));
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

        loginService.resetInternalPassword(id, password);
        return ResponseEntity.ok().build();
    }
}