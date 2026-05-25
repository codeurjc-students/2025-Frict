package com.tfg.backend.security.jwt;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.tfg.backend.dto.EventAction;
import com.tfg.backend.dto.UserSignupDTO;
import com.tfg.backend.event.UserEvent;
import com.tfg.backend.model.User;
import com.tfg.backend.security.GoogleTokenDTO;
import com.tfg.backend.service.EmailService;
import com.tfg.backend.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserLoginService {

	private static final Logger log = LoggerFactory.getLogger(UserLoginService.class);

	private final AuthenticationManager authenticationManager;
	private final UserDetailsService userDetailsService;
	private final JwtTokenProvider jwtTokenProvider;

	private final UserService userService;
	private final EmailService emailService;
	private final PasswordEncoder passwordEncoder;
	private final ApplicationEventPublisher eventPublisher;

	@Value("${google.auth.clientId}")
	private String googleClientId;

	@Value("${app.cookie.secure}")
	private boolean cookieSecure;

	@Value("${app.cookie.same-site}")
	private String cookieSameSite;

	@Transactional
	public AuthResponse loginWithGoogle(HttpServletResponse response, GoogleTokenDTO tokenDTO) {
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

		User user;
		boolean existentUser = userService.existsByEmail(email);
		if (existentUser) {
			user = userService.findByEmail(email)
					.orElseThrow(() -> new UsernameNotFoundException("Data integrity error: Corrupt user data"));
		} else {
			String uniqueUuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
			String dummyPassword = passwordEncoder.encode(UUID.randomUUID().toString());
			user = userService.registerUser(new UserSignupDTO(
					(String) payload.get("name"), "google_" + uniqueUuid, dummyPassword, email, "USER"
			));
		}

		UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

		Authentication authentication = new UsernamePasswordAuthenticationToken(
				userDetails, null, userDetails.getAuthorities()
		);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		var newAccessToken = jwtTokenProvider.generateAccessToken(userDetails);
		var newRefreshToken = jwtTokenProvider.generateRefreshToken(userDetails);
		addTokenCookie(response, TokenType.ACCESS, newAccessToken);
		addTokenCookie(response, TokenType.REFRESH, newRefreshToken);

		if (existentUser){
			return new AuthResponse(AuthResponse.Status.SUCCESS, "Successful Google Login");
		}
		else return new AuthResponse(AuthResponse.Status.SUCCESS, "Successful Google Signup", email);
	}


	public AuthResponse login(HttpServletResponse response, LoginRequest loginRequest) {
		if(userService.isBannedByUsername(loginRequest.getUsername())){
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "This user is banned.");
		}
		if(userService.isDeletedByUsername(loginRequest.getUsername())){
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This user has been previously deleted.");
		}

		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
		SecurityContextHolder.getContext().setAuthentication(authentication);

		UserDetails user = userDetailsService.loadUserByUsername(loginRequest.getUsername());
		var newAccessToken = jwtTokenProvider.generateAccessToken(user);
		var newRefreshToken = jwtTokenProvider.generateRefreshToken(user);

		addTokenCookie(response, TokenType.ACCESS, newAccessToken);
		addTokenCookie(response, TokenType.REFRESH, newRefreshToken);

		return new AuthResponse(AuthResponse.Status.SUCCESS, "Auth successful. Tokens are created in cookie.");
	}

	// Igual que login, solo lee y genera tokens.
	public AuthResponse refresh(HttpServletResponse response, String refreshToken) {
		try {
			var claims = jwtTokenProvider.validateToken(refreshToken);
			UserDetails user = userDetailsService.loadUserByUsername(claims.getSubject());
			var newAccessToken = jwtTokenProvider.generateAccessToken(user);
			addTokenCookie(response, TokenType.ACCESS, newAccessToken);
			return new AuthResponse(AuthResponse.Status.SUCCESS, "Auth successful. Tokens are created in cookie.");

		} catch (Exception e) {
			log.error("Error while processing refresh token", e);
			return new AuthResponse(AuthResponse.Status.FAILURE, "Failure while processing refresh token");
		}
	}

	public AuthResponse logout(HttpServletResponse response) {
		SecurityContextHolder.clearContext();
		removeTokenCookie(response, TokenType.ACCESS);
		removeTokenCookie(response, TokenType.REFRESH);
		return new AuthResponse(AuthResponse.Status.SUCCESS, "Logout successfully");
	}

	@Transactional
	public void recoverPassword(String username){
		User user = userService.findUserHelper(username);

		SecureRandom secureRandom = new SecureRandom();
		String newOtp = String.format("%06d", secureRandom.nextInt(1000000));
		user.setOtpCode(newOtp);
		user.setOtpExpiration(LocalDateTime.now().plusMinutes(15));

		// Saved automatically

		// If this fails, OTP is not stored in DB
		emailService.sendRecoveryOtp(user.getEmail(), user.getUsername(), user.getOtpCode());
	}

	public boolean verifyOtp(String username, String otpCode){
		User user = userService.findUserHelper(username);
		return user.isOtpValid(otpCode);
	}

	@Transactional
	public void resetSelfPassword(String username, String otpCode, String newPassword){
		User user = userService.findUserHelper(username);

		if (!user.isOtpValid(otpCode)){
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "OTP code does not match or is expired.");
		}

		user.setEncodedPassword(passwordEncoder.encode(newPassword));
		user.setOtpCode(null); // Clean OTP after using it
		user.setOtpExpiration(null);
		// Saved automatically
	}

	@Transactional
	public void resetInternalPassword(Long id, String newPassword){
		User user = userService.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User with ID " + id + " does not exist."));

		if (user.getRoles().contains("USER")){
			throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "User password cannot be changed by other accounts.");
		}

		user.setEncodedPassword(passwordEncoder.encode(newPassword));

		//Send notifications
		UserEvent userEvent = new UserEvent(EventAction.UPDATED, user.getUsername());
		eventPublisher.publishEvent(userEvent);

		// Saved automatically
	}

	// -- PRIVATE COOKIES METHODS --
	private void addTokenCookie(HttpServletResponse response, TokenType type, String token) {
		ResponseCookie cookie = ResponseCookie.from(type.cookieName, token)
				.maxAge(type.duration)
				.httpOnly(true)
				.secure(cookieSecure)
				.path("/")
				.sameSite(cookieSameSite)
				.build();
		response.addHeader("Set-Cookie", cookie.toString());
	}

	private void removeTokenCookie(HttpServletResponse response, TokenType type) {
		ResponseCookie cookie = ResponseCookie.from(type.cookieName, "")
				.maxAge(0)
				.httpOnly(true)
				.secure(cookieSecure)
				.path("/")
				.sameSite(cookieSameSite)
				.build();
		response.addHeader("Set-Cookie", cookie.toString());
	}
}