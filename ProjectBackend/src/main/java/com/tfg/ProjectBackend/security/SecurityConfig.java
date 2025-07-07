package com.tfg.ProjectBackend.security;

import com.tfg.ProjectBackend.security.jwt.JwtRequestFilter;
import com.tfg.ProjectBackend.security.jwt.UnauthorizedHandlerJwt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Autowired
    public RepositoryUserDetailsService userDetailService;

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

        authProvider.setUserDetailsService(userDetailService);
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    @Bean
    @Order(1) // Esta cadena de filtros se aplica primero
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {

        http.authenticationProvider(authenticationProvider());

        http
                .securityMatcher("/api/**")
                .exceptionHandling(handling -> handling.authenticationEntryPoint(unauthorizedHandlerJwt)); // Manejador de errores de autenticación

        // Habilitar CSRF para la API, usando un repositorio de cookies que JavaScript pueda leer
        http.csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()) // El token CSRF se guarda en una cookie NO HttpOnly para que JS pueda leerla
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()) // Manejador por defecto para poner el token en el request attribute
        );

        http
                .authorizeHttpRequests(authorize -> authorize
                        // ENDPOINTS PÚBLICOS (no requieren autenticación ni JWT)
                        .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()

                        // ENDPOINTS PRIVADOS (requieren autenticación con JWT)
                        .requestMatchers("/api/auth/available-doctors", "/api/user/role", "/api/user/profile").authenticated()
                        .requestMatchers("/api/auth/logout").authenticated() // Logout también es una acción protegida

                        // Cualquier otra solicitud a /api/** requiere autenticación por defecto
                        .anyRequest().authenticated()
                );


        // Deshabilitar la autenticación por formulario (ya que es una API REST con JWT)
        http.formLogin(formLogin -> formLogin.disable());

        // Deshabilitar la autenticación Basic (ya que es una API REST con JWT)
        http.httpBasic(httpBasic -> httpBasic.disable());

        // Configurar la gestión de sesión como STATELESS (sin estado), esencial para JWT
        http.sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // Filtro JWT antes del filtro de autenticación de usuario/contraseña de Spring Security
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}