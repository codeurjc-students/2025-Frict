package com.tfg.backend.api;

import com.tfg.backend.BackendApplication;
import com.tfg.backend.model.User;
import com.tfg.backend.repository.UserRepository;
import com.tfg.backend.security.jwt.LoginRequest;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import static org.hamcrest.Matchers.*;

@SpringBootTest(
        classes = BackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
public class StatApiFunctionalITest {

    @LocalServerPort
    int port;

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private EntityManager entityManager;

    private static final String JWT_COOKIE_NAME = "AuthToken";
    private static final String BASE_URL_AUTH = "/api/v1/auth";
    private static final String BASE_URL_STATS = "/api/v1/stats";

    private User testAdmin;
    private User testManager;
    private User testDriver;
    private User testUser;

    private String adminCookie;
    private String managerCookie;
    private String driverCookie;
    private String userCookie;

    @BeforeEach
    public void setUp() {
        RestAssured.reset();
        RestAssured.baseURI = "https://localhost:" + port;
        RestAssured.useRelaxedHTTPSValidation();

        // 1. Temporarily bypass security for Hibernate to see everything
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "pass",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );

        cleanDatabase();
        SecurityContextHolder.clearContext();

        // 2. USER CREATION
        testAdmin = new User("Admin", "admin_stat", "admin@stat.com", passwordEncoder.encode("pass"), "ADMIN");
        userRepository.saveAndFlush(testAdmin);

        testManager = new User("Manager", "manager_stat", "manager@stat.com", passwordEncoder.encode("pass"), "MANAGER");
        userRepository.saveAndFlush(testManager);

        testDriver = new User("Driver", "driver_stat", "driver@stat.com", passwordEncoder.encode("pass"), "DRIVER");
        userRepository.saveAndFlush(testDriver);

        testUser = new User("User", "user_stat", "user@stat.com", passwordEncoder.encode("pass"), "USER");
        userRepository.saveAndFlush(testUser);

        // 3. COOKIE CACHING
        adminCookie = loginAndGetCookie(testAdmin.getUsername(), "pass");
        managerCookie = loginAndGetCookie(testManager.getUsername(), "pass");
        driverCookie = loginAndGetCookie(testDriver.getUsername(), "pass");
        userCookie = loginAndGetCookie(testUser.getUsername(), "pass");
    }

    @AfterEach
    public void tearDown() {
        cleanDatabase();
    }

    /**
     * CLEAN DATABASE (TRADITIONAL):
     * Safely cleans the database respecting the JPA lifecycle and
     * breaking relationships to avoid Foreign Key exceptions.
     */
    private void cleanDatabase() {
        transactionTemplate.executeWithoutResult(status -> {
            // 1. Delete dependent (child) entities using direct JPQL for speed
            entityManager.createQuery("DELETE FROM OrderItem").executeUpdate();
            entityManager.createQuery("DELETE FROM Order").executeUpdate();
            entityManager.createQuery("DELETE FROM Review").executeUpdate();
            entityManager.createQuery("DELETE FROM ShopStock").executeUpdate();

            // 2. Clear ManyToMany intermediate tables through Hibernate
            userRepository.findAll().forEach(u -> {
                u.getFavouriteProducts().clear();
                u.setSelectedShop(null);
                userRepository.save(u);
            });
            userRepository.flush();

            // 3. Break circular references of Shops and Trucks
            entityManager.createQuery("UPDATE Truck t SET t.assignedDriver = null, t.assignedShop = null").executeUpdate();
            entityManager.createQuery("UPDATE Shop s SET s.assignedManager = null").executeUpdate();

            // 4. Delete parent entities
            entityManager.createQuery("DELETE FROM Truck").executeUpdate();
            userRepository.deleteAll(); // Use the repository here to cascade deletes (cards, etc)
            entityManager.createQuery("DELETE FROM Shop").executeUpdate();
            entityManager.createQuery("DELETE FROM Product").executeUpdate();
            entityManager.createQuery("DELETE FROM Category").executeUpdate();
            entityManager.createQuery("DELETE FROM Address").executeUpdate();

            // 5. Clear memory
            entityManager.flush();
            entityManager.clear();
        });
    }

    // --- REFACTORED AUTHENTICATION HELPERS ---

    private String loginAndGetCookie(String username, String password) {
        return given().contentType(ContentType.JSON).body(new LoginRequest(username, password))
                .when().post(BASE_URL_AUTH + "/login").getCookie(JWT_COOKIE_NAME);
    }

    private RequestSpecification authAsAdmin() { return new RequestSpecBuilder().setBasePath(BASE_URL_STATS).setContentType(ContentType.JSON).addCookie(JWT_COOKIE_NAME, adminCookie).build(); }
    private RequestSpecification authAsManager() { return new RequestSpecBuilder().setBasePath(BASE_URL_STATS).setContentType(ContentType.JSON).addCookie(JWT_COOKIE_NAME, managerCookie).build(); }
    private RequestSpecification authAsDriver() { return new RequestSpecBuilder().setBasePath(BASE_URL_STATS).setContentType(ContentType.JSON).addCookie(JWT_COOKIE_NAME, driverCookie).build(); }
    private RequestSpecification authAsUser() { return new RequestSpecBuilder().setBasePath(BASE_URL_STATS).setContentType(ContentType.JSON).addCookie(JWT_COOKIE_NAME, userCookie).build(); }

    // ==========================================
    // ORDERS STATISTICS TESTS
    // ==========================================

    @Test
    public void getOrdersStats_AsAdmin_ReturnsList() {
        given().spec(authAsAdmin())
                .when().get("/orders")
                .then().statusCode(200)
                .body("size()", greaterThan(0))
                .body("[0].label", notNullValue())
                .body("[0].value", notNullValue());
    }

    @Test
    public void getOrdersStats_AsManager_ReturnsList() {
        given().spec(authAsManager())
                .when().get("/orders")
                .then().statusCode(200)
                .body("size()", greaterThan(0))
                .body("[0].label", notNullValue())
                .body("[0].value", notNullValue());
    }

    @Test
    public void getOrdersStats_AsDriver_ReturnsList() {
        given().spec(authAsDriver())
                .when().get("/orders")
                .then().statusCode(200)
                .body("size()", greaterThan(0))
                .body("[0].label", notNullValue())
                .body("[0].value", notNullValue());
    }

    // ==========================================
    // SHOPS STATISTICS TESTS
    // ==========================================

    @Test
    public void getShopsStats_AsAdmin_ReturnsList() {
        given().spec(authAsAdmin())
                .when().get("/shops")
                .then().statusCode(200)
                .body("size()", greaterThan(0))
                .body("[0].label", notNullValue())
                .body("[0].value", notNullValue());
    }

    @Test
    public void getShopsStats_AsManager_ReturnsList() {
        given().spec(authAsManager())
                .when().get("/shops")
                .then().statusCode(200)
                .body("size()", greaterThan(0))
                .body("[0].label", notNullValue())
                .body("[0].value", notNullValue());
    }

    // ==========================================
    // TRUCKS STATISTICS TESTS
    // ==========================================

    @Test
    public void getTrucksStats_AsAdmin_ReturnsList() {
        given().spec(authAsAdmin())
                .when().get("/trucks")
                .then().statusCode(200)
                .body("size()", greaterThan(0))
                .body("[0].label", notNullValue())
                .body("[0].value", notNullValue());
    }

    @Test
    public void getTrucksStats_AsManager_ReturnsList() {
        given().spec(authAsManager())
                .when().get("/trucks")
                .then().statusCode(200)
                .body("size()", greaterThan(0))
                .body("[0].label", notNullValue())
                .body("[0].value", notNullValue());
    }
}