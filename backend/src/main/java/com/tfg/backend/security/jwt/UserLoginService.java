package com.tfg.backend.security.jwt;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.tfg.backend.dto.UserSignupDTO;
import com.tfg.backend.model.User;
import com.tfg.backend.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserLoginService {

	private static final Logger log = LoggerFactory.getLogger(UserLoginService.class);

	private final AuthenticationManager authenticationManager;
	private final UserDetailsService userDetailsService;
	private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

	public UserLoginService(AuthenticationManager authenticationManager, UserDetailsService userDetailsService, JwtTokenProvider jwtTokenProvider) {
		this.authenticationManager = authenticationManager;
		this.userDetailsService = userDetailsService;
		this.jwtTokenProvider = jwtTokenProvider;
	}

    public AuthResponse loginWithGoogle(HttpServletResponse response, GoogleIdToken.Payload payload) {
        String email = payload.getEmail();
        User user;

        if (userService.existsByEmail(email)) {
            user = userService.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Data integrity error: Corrupt user data"));
        } else {
            String uniqueUuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            String dummyPassword = passwordEncoder.encode(UUID.randomUUID().toString()); // Trash secure password (never used)

            user = userService.registerUser(new UserSignupDTO(
                    (String) payload.get("name"), //Real Google name
                    "google_" + uniqueUuid,
                    dummyPassword,
                    email,
                    "USER"
            ));
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername()); // O email, según tu config

        // Bypass AuthenticationManager and trust the valid Google token only
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null, // No credentials to show
                userDetails.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate and add session JWT tokens
        var newAccessToken = jwtTokenProvider.generateAccessToken(userDetails);
        var newRefreshToken = jwtTokenProvider.generateRefreshToken(userDetails);
        response.addCookie(buildTokenCookie(TokenType.ACCESS, newAccessToken));
        response.addCookie(buildTokenCookie(TokenType.REFRESH, newRefreshToken));

        return new AuthResponse(AuthResponse.Status.SUCCESS, "Successful Google Login");
    }

	public AuthResponse login(HttpServletResponse response, LoginRequest loginRequest) {
		
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

		SecurityContextHolder.getContext().setAuthentication(authentication);

		
		String username = loginRequest.getUsername();
		UserDetails user = userDetailsService.loadUserByUsername(username);

		var newAccessToken = jwtTokenProvider.generateAccessToken(user);
		var newRefreshToken = jwtTokenProvider.generateRefreshToken(user);

		response.addCookie(buildTokenCookie(TokenType.ACCESS, newAccessToken));
		response.addCookie(buildTokenCookie(TokenType.REFRESH, newRefreshToken));

		return new AuthResponse(AuthResponse.Status.SUCCESS, "Auth successful. Tokens are created in cookie.");
	}

	public ResponseEntity<AuthResponse> refresh(HttpServletResponse response, String refreshToken) {
		try {
			var claims = jwtTokenProvider.validateToken(refreshToken);
			UserDetails user = userDetailsService.loadUserByUsername(claims.getSubject());

			var newAccessToken = jwtTokenProvider.generateAccessToken(user);
			response.addCookie(buildTokenCookie(TokenType.ACCESS, newAccessToken));

			AuthResponse loginResponse = new AuthResponse(AuthResponse.Status.SUCCESS,
					"Auth successful. Tokens are created in cookie.");
			return ResponseEntity.ok().body(loginResponse);

		} catch (Exception e) {
			log.error("Error while processing refresh token", e);
			AuthResponse loginResponse = new AuthResponse(AuthResponse.Status.FAILURE,
					"Failure while processing refresh token");
			return ResponseEntity.ok().body(loginResponse);
		}
	}

	public String logout(HttpServletResponse response) {
		SecurityContextHolder.clearContext();
		response.addCookie(removeTokenCookie(TokenType.ACCESS));
		response.addCookie(removeTokenCookie(TokenType.REFRESH));

		return "logout successfully";
	}

	private Cookie buildTokenCookie(TokenType type, String token) {
		Cookie cookie = new Cookie(type.cookieName, token);
		cookie.setMaxAge((int) type.duration.getSeconds());
		cookie.setHttpOnly(true);
        cookie.setSecure(true);
		cookie.setPath("/");
		return cookie;
	}

	private Cookie removeTokenCookie(TokenType type){
		Cookie cookie = new Cookie(type.cookieName, "");
		cookie.setMaxAge(0);
		cookie.setHttpOnly(true);
        cookie.setSecure(true);
		cookie.setPath("/");
		return cookie;
	}
}
