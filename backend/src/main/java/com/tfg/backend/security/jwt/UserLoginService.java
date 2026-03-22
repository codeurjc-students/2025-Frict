package com.tfg.backend.security.jwt;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.tfg.backend.dto.UserSignupDTO;
import com.tfg.backend.model.User;
import com.tfg.backend.security.GoogleTokenDTO;
import com.tfg.backend.service.EmailService;
import com.tfg.backend.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
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
	private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

	@Value("${google.auth.clientId}")
	private String googleClientId;

	public UserLoginService(AuthenticationManager authenticationManager, UserDetailsService userDetailsService, JwtTokenProvider jwtTokenProvider) {
		this.authenticationManager = authenticationManager;
		this.userDetailsService = userDetailsService;
		this.jwtTokenProvider = jwtTokenProvider;
	}


	//As it may act as
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
            String dummyPassword = passwordEncoder.encode(UUID.randomUUID().toString()); // Trash secure password (never used)

            user = userService.registerUser(new UserSignupDTO(
                    (String) payload.get("name"), //Real Google name
                    "google_" + uniqueUuid,
                    dummyPassword,
                    email,
                    "USER"
            ));
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

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

		if (existentUser){
			return new AuthResponse(AuthResponse.Status.SUCCESS, "Successful Google Login");
		}
		else return new AuthResponse(AuthResponse.Status.SUCCESS, "Successful Google Signup", email);
    }

	public AuthResponse login(HttpServletResponse response, LoginRequest loginRequest) {

		//Check if the user is banned or deleted
		if(userService.isBannedByUsername(loginRequest.getUsername())){
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "This user is banned.");
		}

		if(userService.isDeletedByUsername(loginRequest.getUsername())){
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This user has been previously deleted.");
		}
		
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

	public AuthResponse refresh(HttpServletResponse response, String refreshToken) {
		try {
			var claims = jwtTokenProvider.validateToken(refreshToken);
			UserDetails user = userDetailsService.loadUserByUsername(claims.getSubject());

			var newAccessToken = jwtTokenProvider.generateAccessToken(user);
			response.addCookie(buildTokenCookie(TokenType.ACCESS, newAccessToken));
			return new AuthResponse(AuthResponse.Status.SUCCESS, "Auth successful. Tokens are created in cookie.");

		} catch (Exception e) {
			log.error("Error while processing refresh token", e);
			return new AuthResponse(AuthResponse.Status.FAILURE, "Failure while processing refresh token");
		}
	}

	public AuthResponse logout(HttpServletResponse response) {
		SecurityContextHolder.clearContext();
		response.addCookie(removeTokenCookie(TokenType.ACCESS));
		response.addCookie(removeTokenCookie(TokenType.REFRESH));
		return new AuthResponse(AuthResponse.Status.SUCCESS, "Logout successfully");
	}

	public void recoverPassword(String username){
		User user = userService.findUserHelper(username);

		SecureRandom secureRandom = new SecureRandom();
		String newOtp = String.format("%06d", secureRandom.nextInt(1000000)); //6-digit OTP
		user.setOtpCode(newOtp);
		user.setOtpExpiration(LocalDateTime.now().plusMinutes(15));
		User savedUser = userService.save(user);

		emailService.sendRecoveryOtp(savedUser.getEmail(), savedUser.getUsername(), savedUser.getOtpCode());
	}

	public boolean verifyOtp(String username, String otpCode){
		User user = userService.findUserHelper(username);
		if (!user.isOtpValid(otpCode)){
			return false;
		}
		user.setOtpCode(null);
		user.setOtpExpiration(null);
		userService.save(user);
		return true;
	}

	public void resetSelfPassword(String username, String otpCode, String newPassword){
		User user = userService.findUserHelper(username);

		if (!user.isOtpValid(otpCode)){
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "OTP code does not match or is expired.");
		}

		user.setEncodedPassword(passwordEncoder.encode(newPassword));
		userService.save(user);
	}

	public void resetInternalPassword(Long id, String newPassword){
		Optional<User> userOptional = userService.findById(id);

		if (userOptional.isEmpty()){
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User with ID " + id + " does not exist.");
		}
		User user = userOptional.get();

		if (user.getRoles().contains("USER")){
			throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "User password cannot be changed by other accounts.");
		}

		user.setEncodedPassword(passwordEncoder.encode(newPassword));
		userService.save(user);
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
