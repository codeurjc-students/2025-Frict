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
                .securityMatcher("/api/**")
                .authorizeHttpRequests(authorize -> authorize

                        // --- 1. AUTHENTICATION (AuthRestController) ---
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/recovery", "/api/v1/auth/verification", "/api/v1/auth/reset").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh", "/api/v1/auth/logout").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/google", "/api/v1/auth/signup").permitAll() // Públicos por lógica de negocio, aunque marcados como (User)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/auth/reset/*").hasRole("ADMIN")

                        // --- 2. CATEGORIES (CategoryRestController) ---
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll() // (All)
                        .requestMatchers("/api/v1/categories/**").hasRole("ADMIN") // (Admin) para POST, PUT, DELETE e imágenes

                        // --- 3. PRODUCTS (ProductRestController) ---
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/filter", "/api/v1/products/{id}", "/api/v1/products/stock/*").permitAll() // (All)
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/favourites/**").hasRole("USER") // (User)
                        .requestMatchers(HttpMethod.POST, "/api/v1/products/favourites/*").hasRole("USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products/favourites/*").hasRole("USER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/available/*").hasRole("MANAGER") // (Manager)
                        .requestMatchers("/api/v1/products/**").hasRole("ADMIN") // (Admin) para CRUD, imágenes y activaciones

                        // --- 4. ORDERS & CART (OrderRestController) ---
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/cart/**").hasRole("USER") // (User)
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders/cart/**").hasRole("USER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/orders/cart/**").hasRole("USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/orders/cart/**").hasRole("USER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders").hasRole("USER") // (User) Crear pedido
                        .requestMatchers(HttpMethod.PUT, "/api/v1/orders/cancel/*").hasRole("USER") // (User) Cancelar
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/{id}").hasAnyRole("USER", "MANAGER", "DRIVER", "ADMIN") // (User, etc.)
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/user/*").hasRole("ADMIN") // (Admin) Pedidos de un usuario
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/orders/*").hasRole("ADMIN") // (Admin) Borrar finalizados
                        .requestMatchers(HttpMethod.PUT, "/api/v1/orders/*").hasAnyRole("ADMIN", "MANAGER") // (Admin, Manager) Actualizar estado
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders/*/assign/truck/*").hasAnyRole("ADMIN", "MANAGER") // (Admin, Manager) Asignar camión
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/").hasAnyRole("ADMIN", "MANAGER", "DRIVER") // Endpoint para roles internos
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders").hasRole("USER") // Endpoint exclusivo de clientes

                        // --- 5. REVIEWS (ReviewRestController) ---
                        .requestMatchers(HttpMethod.GET, "/api/v1/reviews/").permitAll() // (All) Reviews de producto
                        .requestMatchers(HttpMethod.GET, "/api/v1/reviews").hasAnyRole("USER", "ADMIN") // (All/User) Propias y Admin
                        .requestMatchers(HttpMethod.GET, "/api/v1/reviews/user/*").hasRole("ADMIN") // (Admin)
                        .requestMatchers(HttpMethod.POST, "/api/v1/reviews").hasRole("USER") // (User)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/reviews").hasRole("USER") // (User)
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/reviews/*").hasAnyRole("USER", "ADMIN", "MANAGER") // (Admin, User)

                        // --- 6. SHOPS (ShopRestController) ---
                        .requestMatchers(HttpMethod.GET, "/api/v1/shops/list").hasAnyRole("USER", "ADMIN") // (User)
                        .requestMatchers(HttpMethod.GET, "/api/v1/shops/truck/*").hasRole("DRIVER") // (Driver)
                        .requestMatchers(HttpMethod.GET, "/api/v1/shops/stock/*").permitAll() // (All)
                        .requestMatchers(HttpMethod.POST, "/api/v1/shops").hasRole("ADMIN") // (Admin) Crear
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/shops/*", "/api/v1/shops/image/*").hasRole("ADMIN") // (Admin) Borrar
                        .requestMatchers(HttpMethod.PUT, "/api/v1/shops/*/assign/manager/*").hasRole("ADMIN") // (Admin) Asignar manager
                        .requestMatchers(HttpMethod.GET, "/api/v1/shops/").hasRole("ADMIN") // Todas las tiendas
                        .requestMatchers(HttpMethod.GET, "/api/v1/shops").hasRole("MANAGER") // Tiendas asignadas al manager
                        .requestMatchers(HttpMethod.GET, "/api/v1/shops/{id}").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/shops/{id}", "/api/v1/shops/image/*").hasAnyRole("ADMIN", "MANAGER") // (Admin, Manager)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/shops/active/*", "/api/v1/shops/*/active/", "/api/v1/shops/restock/*", "/api/v1/shops/*/assign/stock/*", "/api/v1/shops/*/assign/truck/*").hasRole("MANAGER") // (Manager)

                        // --- 7. TRUCKS (TruckRestController) ---
                        .requestMatchers(HttpMethod.GET, "/api/v1/trucks/shop/*").permitAll() // (All)
                        .requestMatchers(HttpMethod.GET, "/api/v1/trucks/user/*").hasRole("DRIVER") // (Driver)
                        .requestMatchers(HttpMethod.GET, "/api/v1/trucks/", "/api/v1/trucks/available/").hasAnyRole("ADMIN", "MANAGER") // (Admin) All vs (Admin, Manager) Available
                        .requestMatchers(HttpMethod.GET, "/api/v1/trucks/{id}", "/api/v1/trucks/shop/*/list").hasAnyRole("ADMIN", "MANAGER") // (Admin, Manager)
                        .requestMatchers(HttpMethod.POST, "/api/v1/trucks").hasRole("ADMIN") // (Admin)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/trucks/**").hasRole("ADMIN") // (Admin) CRUD y asignaciones
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/trucks/*").hasRole("ADMIN") // (Admin)

                        // --- 8. USERS (UserRestController) ---
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/session", "/api/v1/users/me", "/api/v1/users/username", "/api/v1/users/email").permitAll() // (All)
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/", "/api/v1/users/drivers/available/", "/api/v1/users/role/", "/api/v1/users/stats").hasRole("ADMIN") // (Admin)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/users/ban/**", "/api/v1/users/anon/**").hasRole("ADMIN") // (Admin)
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/users/", "/api/v1/users/*").hasRole("ADMIN") // (Admin)
                        .requestMatchers("/api/v1/users/**").authenticated() // (User) Direcciones, tarjetas, imagen, datos propios

                        // --- 9. STATS (StatRestController) ---
                        .requestMatchers("/api/v1/stats/orders").hasAnyRole("ADMIN", "MANAGER", "DRIVER") // (Admin, Manager, Driver)
                        .requestMatchers("/api/v1/stats/shops", "/api/v1/stats/trucks").hasAnyRole("ADMIN", "MANAGER") // (Admin, Manager)

                        .anyRequest().denyAll() // Bloqueo total por defecto para máxima seguridad
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