package com.tfg.backend.api;

import com.tfg.backend.BackendApplication;
import com.tfg.backend.repository.*;
import com.tfg.backend.security.jwt.LoginRequest;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static io.restassured.RestAssured.given;

/**
 * Base abstract class for all API functional integration tests.
 * Centralizes Spring Boot context, RestAssured configuration, authentication helpers,
 * and a universal safe database cleanup method.
 */
@SpringBootTest(
        classes = BackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
public abstract class BaseApiFunctionalITest {

    @LocalServerPort
    protected int port;

    // --- SHARED REPOSITORIES ---
    @Autowired protected UserRepository userRepository;
    @Autowired protected ShopRepository shopRepository;
    @Autowired protected TruckRepository truckRepository;
    @Autowired protected ProductRepository productRepository;
    @Autowired protected CategoryRepository categoryRepository;
    @Autowired protected ReviewRepository reviewRepository;
    @Autowired protected ShopStockRepository shopStockRepository;

    // --- SHARED UTILS ---
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected TransactionTemplate transactionTemplate;
    @Autowired protected EntityManager entityManager;

    protected static final String JWT_COOKIE_NAME = "AuthToken";
    protected static final String BASE_URL_AUTH = "/api/v1/auth";

    /**
     * Executes BEFORE the @BeforeEach of the child classes.
     * Prepares RestAssured and guarantees a pristine database.
     */
    @BeforeEach
    public void baseSetUp() {
        RestAssured.reset();
        RestAssured.baseURI = "http://localhost:" + port;

        // Bypass security to allow Hibernate to see all entities (bypassing @Filter for inactive products, etc.)
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "pass",
                        List.of(new SimpleGrantedAuthority("ADMIN")))
        );

        globalCleanDatabase();
        SecurityContextHolder.clearContext();
    }

    /**
     * Executes AFTER the @AfterEach of the child classes as a fallback.
     */
    @AfterEach
    public void baseTearDown() {
        globalCleanDatabase();
    }

    /**
     * Universal database cleanup. Handles all entities and relationships across the entire system
     * to prevent TransientObjectExceptions and Foreign Key Constraint violations.
     */
    protected void globalCleanDatabase() {
        transactionTemplate.executeWithoutResult(status -> {
            entityManager.clear(); // Clear persistence context

            // 1. Delete standalone dependent entities directly via JPQL for speed
            entityManager.createQuery("DELETE FROM OrderItem").executeUpdate();
            entityManager.createQuery("DELETE FROM Order").executeUpdate();
            entityManager.createQuery("DELETE FROM Review").executeUpdate();
            entityManager.createQuery("DELETE FROM ShopStock").executeUpdate();

            // 2. Unlink Direct Foreign Keys in DB
            entityManager.createQuery("UPDATE Truck t SET t.assignedDriver = null, t.assignedShop = null").executeUpdate();
            entityManager.createQuery("UPDATE Shop s SET s.assignedManager = null").executeUpdate();

            entityManager.flush();
            entityManager.clear();

            // 3. Unlink ManyToMany via JPA (required for joint tables like user_favourite_products)
            userRepository.findAll().forEach(u -> {
                u.setSelectedShop(null);
                u.getFavouriteProducts().clear();
                userRepository.save(u);
            });
            userRepository.flush();

            // 4. Safely delete main entities using repositories to respect CascadeType.ALL (Addresses, Logs, Cards)
            truckRepository.deleteAll();
            shopRepository.deleteAll();
            userRepository.deleteAll();
            productRepository.deleteAll();
            categoryRepository.deleteAll();

            // 5. Fallback for orphaned addresses
            entityManager.createQuery("DELETE FROM Address").executeUpdate();

            entityManager.flush();
            entityManager.clear();
        });
    }

    // ==========================================
    // AUTHENTICATION HELPERS
    // ==========================================

    /**
     * Performs a login request and extracts the JWT cookie.
     */
    protected String loginAndGetCookie(String username, String password) {
        return given().contentType(ContentType.JSON).body(new LoginRequest(username, password))
                .when().post(BASE_URL_AUTH + "/login").getCookie(JWT_COOKIE_NAME);
    }

    /**
     * Generates a RequestSpecification with the desired base path and injected JWT cookie.
     */
    protected RequestSpecification getSpec(String basePath, String cookie) {
        RequestSpecBuilder builder = new RequestSpecBuilder()
                .setBasePath(basePath)
                .setContentType(ContentType.JSON);

        if (cookie != null && !cookie.isEmpty()) {
            builder.addCookie(JWT_COOKIE_NAME, cookie);
        }
        return builder.build();
    }
}