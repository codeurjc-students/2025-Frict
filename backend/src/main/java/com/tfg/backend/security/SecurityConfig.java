package com.tfg.backend.security;

import com.tfg.backend.security.jwt.JwtRequestFilter;
import com.tfg.backend.security.jwt.UnauthorizedHandlerJwt;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;
    private final RepositoryUserDetailsService userDetailsService;
    private final UnauthorizedHandlerJwt unauthorizedHandlerJwt;

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
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .crossOriginOpenerPolicy(coop ->
                                coop.policy(CrossOriginOpenerPolicyHeaderWriter.CrossOriginOpenerPolicy.SAME_ORIGIN_ALLOW_POPUPS)
                        )
                )
                .sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize

                        // --- 1. AUTHENTICATION (AuthRestController) ---
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/recovery", "/api/v1/auth/verification", "/api/v1/auth/reset").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh", "/api/v1/auth/logout").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/google", "/api/v1/auth/signup").permitAll() // Públicos por lógica de negocio, aunque marcados como (User)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/auth/reset/*").hasAuthority("ADMIN")

                        // --- 2. CATEGORIES (CategoryRestController) ---
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll() // (All)
                        .requestMatchers("/api/v1/categories/**").hasAuthority("ADMIN") // (Admin) para POST, PUT, DELETE e imágenes

                        // --- 3. PRODUCTS (ProductRestController) ---
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/filter", "/api/v1/products/{id}", "/api/v1/products/stock/*").permitAll() // (All)
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/favourites/**").hasAuthority("USER") // (User)
                        .requestMatchers(HttpMethod.POST, "/api/v1/products/favourites/*").hasAuthority("USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products/favourites/*").hasAuthority("USER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/available/*").hasAuthority("MANAGER") // (Manager)
                        .requestMatchers("/api/v1/products/**").hasAuthority("ADMIN") // (Admin) for CRUD, images and activations

                        // --- 4. ORDERS & CART (OrderRestController) ---
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/cart/**").hasAuthority("USER") // (User)
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders/cart/**").hasAuthority("USER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/orders/cart/**").hasAuthority("USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/orders/cart/**").hasAuthority("USER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders").hasAuthority("USER") // (User) Create order
                        .requestMatchers(HttpMethod.PUT, "/api/v1/orders/cancel/*").hasAuthority("USER") // (User) Cancel order
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/{id}").hasAnyAuthority("USER", "MANAGER", "DRIVER", "ADMIN") // (User, etc.)
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/{id}/token").hasAnyAuthority("USER") // Order confirmation QR codes
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders/{id}/token").hasAnyAuthority("DRIVER") // Order confirmation QR codes
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/user/*").hasAuthority("ADMIN") // (Admin) User orders
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/orders/*").hasAuthority("ADMIN") // (Admin) Clear finished orders
                        .requestMatchers(HttpMethod.PUT, "/api/v1/orders/*").hasAnyAuthority("ADMIN", "MANAGER", "DRIVER") // (Admin, Manager, Driver) Update order status
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders/*/assign/truck/*").hasAnyAuthority("ADMIN", "MANAGER") // (Admin, Manager) Assign truck
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders/*/unassign").hasAnyAuthority("DRIVER") // (Driver) Unassign truck as order is finished (completed or cancelled)
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/").hasAnyAuthority("ADMIN", "MANAGER", "DRIVER") // Internal roles endpoint
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders").hasAuthority("USER") // Client exclusive endpoint

                        // --- 5. REVIEWS (ReviewRestController) ---
                        .requestMatchers(HttpMethod.GET, "/api/v1/reviews/").permitAll() // (All) Product reviews
                        .requestMatchers(HttpMethod.GET, "/api/v1/reviews").hasAnyAuthority("USER", "ADMIN") // (All/User) Reviews for logged user and for admin
                        .requestMatchers(HttpMethod.GET, "/api/v1/reviews/user/*").hasAuthority("ADMIN") // (Admin)
                        .requestMatchers(HttpMethod.POST, "/api/v1/reviews").hasAuthority("USER") // (User)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/reviews").hasAuthority("USER") // (User)
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/reviews/*").hasAnyAuthority("USER", "ADMIN", "MANAGER") // (Admin, User)

                        // --- 6. SHOPS (ShopRestController) ---
                        .requestMatchers(HttpMethod.GET, "/api/v1/shops/list").hasAnyAuthority("USER", "ADMIN") // (User)
                        .requestMatchers(HttpMethod.GET, "/api/v1/shops/references").hasAnyAuthority("MANAGER") // (Manager)
                        .requestMatchers(HttpMethod.GET, "/api/v1/shops/truck/*").hasAuthority("DRIVER") // (Driver)
                        .requestMatchers(HttpMethod.GET, "/api/v1/shops/stock/*").permitAll() // (All)
                        .requestMatchers(HttpMethod.POST, "/api/v1/shops").hasAuthority("ADMIN") // (Admin) Create shop
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/shops/*", "/api/v1/shops/image/*").hasAuthority("ADMIN") // (Admin) Delete shop
                        .requestMatchers(HttpMethod.PUT, "/api/v1/shops/*/assign/manager/*").hasAuthority("ADMIN") // (Admin) Assign manager
                        .requestMatchers(HttpMethod.GET, "/api/v1/shops/").hasAuthority("ADMIN") // All shops
                        .requestMatchers(HttpMethod.GET, "/api/v1/shops").hasAuthority("MANAGER") // Shops assigned to manager
                        .requestMatchers(HttpMethod.GET, "/api/v1/shops/{id}").hasAnyAuthority("ADMIN", "MANAGER", "USER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/shops/{id}", "/api/v1/shops/image/*").hasAnyAuthority("ADMIN", "MANAGER") // (Admin, Manager)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/shops/active/*", "/api/v1/shops/*/active/", "/api/v1/shops/restock/*", "/api/v1/shops/*/assign/stock/*", "/api/v1/shops/*/assign/truck/*").hasAuthority("MANAGER") // (Manager)

                        // --- 7. TRUCKS (TruckRestController) ---
                        .requestMatchers(HttpMethod.GET, "/api/v1/trucks/shop/*").permitAll() // (All)
                        .requestMatchers(HttpMethod.GET, "/api/v1/trucks/user/*").hasAuthority("DRIVER") // (Driver)
                        .requestMatchers(HttpMethod.GET, "/api/v1/trucks/", "/api/v1/trucks/available/").hasAnyAuthority("ADMIN", "MANAGER") // (Admin) All vs (Admin, Manager) Available
                        .requestMatchers(HttpMethod.GET, "/api/v1/trucks/{id}", "/api/v1/trucks/shop/*/list").hasAnyAuthority("ADMIN", "MANAGER") // (Admin, Manager)
                        .requestMatchers(HttpMethod.POST, "/api/v1/trucks").hasAuthority("ADMIN") // (Admin)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/trucks/**").hasAuthority("ADMIN") // (Admin) CRUD and assignments
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/trucks/*").hasAuthority("ADMIN") // (Admin)

                        // --- 8. USERS (UserRestController) ---
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/session", "/api/v1/users/me", "/api/v1/users/username", "/api/v1/users/email").permitAll() // (All)
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/", "/api/v1/users/drivers/available/", "/api/v1/users/role/", "/api/v1/users/stats").hasAuthority("ADMIN") // (Admin)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/users/ban/**", "/api/v1/users/anon/**").hasAuthority("ADMIN") // (Admin)
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/users/", "/api/v1/users/*").hasAuthority("ADMIN") // (Admin)
                        .requestMatchers("/api/v1/users/**").authenticated() // (User) Addresses, cards, images and own personal data

                        // --- 9. STATS (StatRestController) ---
                        .requestMatchers("/api/v1/stats/orders").hasAnyAuthority("ADMIN", "MANAGER", "DRIVER") // (Admin, Manager, Driver)
                        .requestMatchers("/api/v1/stats/shops", "/api/v1/stats/trucks").hasAnyAuthority("ADMIN", "MANAGER") // (Admin, Manager)

                        // --- 10. WEBSOCKETS, NOTIFICATIONS AND REGISTRIES ---
                        .requestMatchers("/api/v1/ws/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/notifications/test").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/notifications/unread", "/api/v1/notifications/").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/notifications/*/read", "/api/v1/notifications/read-all").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/notifications/*").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/registry/private/*").hasAnyAuthority("ADMIN", "MANAGER", "DRIVER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/registry/public/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/registry/private/export/pdf").hasAuthority("ADMIN")

                        .requestMatchers("/api/**").denyAll()
                        .requestMatchers("/**").permitAll()
                )
                .exceptionHandling(handling -> handling.authenticationEntryPoint(unauthorizedHandlerJwt))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("https://localhost:4202"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}