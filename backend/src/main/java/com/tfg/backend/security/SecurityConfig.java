package com.tfg.backend.security;

import com.tfg.backend.security.jwt.JwtRequestFilter;
import com.tfg.backend.security.jwt.UnauthorizedHandlerJwt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.CrossOriginOpenerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Autowired
	private JwtRequestFilter jwtRequestFilter;

	@Autowired
	RepositoryUserDetailsService userDetailsService;

	@Autowired
	private UnauthorizedHandlerJwt unauthorizedHandlerJwt;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
		return authConfig.getAuthenticationManager();
	}

	@Bean
	public DaoAuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

		authProvider.setUserDetailsService(userDetailsService);
		authProvider.setPasswordEncoder(passwordEncoder());

		return authProvider;
	}

    @Bean
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {

        http
                // Explicit CORS configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Diseble CSRF for API REST
                .csrf(AbstractHttpConfigurer::disable)

                // Allow popups to communicate with the application (Google)
                .headers(headers -> headers
                        .crossOriginOpenerPolicy(coop ->
                                coop.policy(CrossOriginOpenerPolicyHeaderWriter.CrossOriginOpenerPolicy.SAME_ORIGIN_ALLOW_POPUPS)
                        )
                )

                // Set session as stateless
                .sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Routes
                .securityMatcher("/api/**")
                .authorizeHttpRequests(authorize -> authorize
                        // AuthRestController
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/google").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/signup").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/recovery").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/verification").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/reset").permitAll()

                        // CategoryRestController
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/categories/*").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/categories/*").hasAnyRole("MANAGER", "ADMIN")

                        // OrderRestController
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders").hasRole("USER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/*").hasAnyRole("USER", "MANAGER", "DRIVER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders").hasRole("USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/orders/*").hasAnyRole("USER", "MANAGER", "DRIVER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/cart/summary").hasRole("USER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/cart").hasRole("USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/orders/cart").hasRole("USER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders/cart/*").hasRole("USER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/orders/cart/*").hasRole("USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/orders/cart/*").hasRole("USER")

                        // ProductRestController
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/filter").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/favourites").hasAnyRole("USER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/favourites/*").hasAnyRole("USER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/stock/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/products/favourites/*").hasAnyRole("USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products/favourites/*").hasAnyRole("USER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/products").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/products/*").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products/*").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/products/*/images").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products/*/images/*").hasAnyRole("MANAGER", "ADMIN")

                        // ReviewRestController
                        .requestMatchers(HttpMethod.GET, "/api/v1/reviews").hasAnyRole("USER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/reviews/").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/reviews").hasAnyRole("USER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/reviews").hasAnyRole("USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/reviews/*").hasAnyRole("USER", "MANAGER", "ADMIN")

                        // UserRestController
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/session").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/users/image/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/users").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/users/avatar").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/users/data").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/users/addresses").hasAnyRole("USER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/users/addresses").hasAnyRole("USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/users/addresses/*").hasAnyRole("USER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/users/cards").hasAnyRole("USER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/users/cards").hasAnyRole("USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/users/cards/*").hasAnyRole("USER")

                        // Public endpoints
                        .anyRequest().permitAll()
                )

                // Exception handling
                .exceptionHandling(handling -> handling.authenticationEntryPoint(unauthorizedHandlerJwt))

                // Authentication provider
                .authenticationProvider(authenticationProvider())

                // JWT filter before user/pass conventional filters
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)

                // Disable unused
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Explicit allowed origins
        configuration.setAllowedOrigins(List.of("https://localhost:4202"));

        // Allowed methods
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Allowed headers
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        // Allow credentials
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
