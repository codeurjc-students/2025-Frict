package com.tfg.backend.api;

import com.tfg.backend.BackendApplication;
import com.tfg.backend.dto.ReviewDTO;
import com.tfg.backend.model.*;
import com.tfg.backend.repository.*;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(
        classes = BackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
public class ReviewApiFunctionalITest {

    @LocalServerPort
    int port;

    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private CategoryRepository categoryRepository;

    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private EntityManager entityManager;

    private static final String JWT_COOKIE_NAME = "AuthToken";
    private static final String BASE_URL_AUTH = "/api/v1/auth";
    private static final String BASE_URL_REVIEWS = "/api/v1/reviews";

    private User testAdmin;
    private User reviewOwner;
    private User otherUser;
    private Product testProduct;
    private Review testReview;

    // Cached cookies to avoid repeated login requests
    private String adminCookie;
    private String ownerCookie;
    private String otherUserCookie;

    @BeforeEach
    public void setUp() {
        RestAssured.reset();
        RestAssured.baseURI = "https://localhost:" + port;
        RestAssured.useRelaxedHTTPSValidation();

        cleanDatabase();

        // 1. Data creation
        Category otrosCategory = new Category("Otros", "icon", "banner", "Desc", "Desc");
        categoryRepository.saveAndFlush(otrosCategory);

        testProduct = new Product("API Review Product", "Desc", 10.0, 5.0);
        testProduct.setReferenceCode("REF-REV-123");
        testProduct.setActive(true);
        testProduct = productRepository.saveAndFlush(testProduct);

        // Create Admin
        testAdmin = new User("Admin", "admin_rev", "admin@rev.com", passwordEncoder.encode("pass"), "ADMIN");
        userRepository.saveAndFlush(testAdmin);

        // Create the user who will own the initial review
        reviewOwner = new User("Owner", "owner_rev", "owner@rev.com", passwordEncoder.encode("pass"), "USER");
        userRepository.saveAndFlush(reviewOwner);

        // Create another regular user to test security violations
        otherUser = new User("Other", "other_rev", "other@rev.com", passwordEncoder.encode("pass"), "USER");
        userRepository.saveAndFlush(otherUser);

        // Create the base review
        testReview = new Review(reviewOwner, testProduct, 5, "Excellent product!", true);
        reviewRepository.saveAndFlush(testReview);

        // 2. Cache login cookies
        adminCookie = loginAndGetCookie(testAdmin.getUsername(), "pass");
        ownerCookie = loginAndGetCookie(reviewOwner.getUsername(), "pass");
        otherUserCookie = loginAndGetCookie(otherUser.getUsername(), "pass");
    }

    @AfterEach
    public void tearDown() {
        cleanDatabase();
    }

    /**
     * Safely cleans the database avoiding foreign key constraint failures.
     */
    private void cleanDatabase() {
        transactionTemplate.executeWithoutResult(status -> {
            reviewRepository.deleteAll();
            productRepository.deleteAll();
            categoryRepository.deleteAll();

            userRepository.findAll().forEach(u -> {
                u.getFavouriteProducts().clear();
                userRepository.save(u);
            });
            userRepository.flush();
            userRepository.deleteAll();

            entityManager.flush();
            entityManager.clear();
        });
    }


    // ==========================================
    // READ ENDPOINTS TESTS
    // ==========================================

    @Test
    public void getAllReviewsByProductId_ReturnsList() {
        given().spec(authAsOtherUser())
                .queryParam("productId", testProduct.getId())
                .when().get("/")
                .then().statusCode(200)
                .body("size()", greaterThan(0))
                .body("[0].text", equalTo("Excellent product!"));
    }

    @Test
    public void getLoggedUserReviews_ReturnsPagedReviews() {
        given().spec(authAsOwner())
                .when().get() // Base path maps to @GetMapping without slashes
                .then().statusCode(200)
                .body("items", notNullValue())
                .body("items.size()", greaterThan(0))
                .body("items[0].creatorId", equalTo(reviewOwner.getId().intValue()));
    }

    // ==========================================
    // CREATION SECURITY & BUSINESS LOGIC
    // ==========================================

    @Test
    public void createReview_ValidUser_CreatesReview() {
        ReviewDTO newReview = new ReviewDTO();
        newReview.setProductId(testProduct.getId());
        newReview.setCreatorId(otherUser.getId()); // Match logged user
        newReview.setRating(4);
        newReview.setText("Very good!");
        newReview.setRecommended(true);

        given().spec(authAsOtherUser())
                .body(newReview)
                .when().post()
                .then().statusCode(201)
                .header("Location", notNullValue())
                .body("text", equalTo("Very good!"));
    }

    @Test
    public void createReview_MismatchCreatorId_ThrowsUnauthorized() {
        ReviewDTO spoofedReview = new ReviewDTO();
        spoofedReview.setProductId(testProduct.getId());
        // Malicious attempt: otherUser tries to create a review masquerading as reviewOwner
        spoofedReview.setCreatorId(reviewOwner.getId());
        spoofedReview.setRating(1);
        spoofedReview.setText("Terrible!");

        given().spec(authAsOtherUser())
                .body(spoofedReview)
                .when().post()
                .then().statusCode(401);
    }

    // ==========================================
    // UPDATE SECURITY & BUSINESS LOGIC
    // ==========================================

    @Test
    public void updateReview_AsCreator_UpdatesReview() {
        ReviewDTO updateDto = new ReviewDTO();
        updateDto.setId(testReview.getId());
        updateDto.setRating(3);
        updateDto.setText("Actually, it is average.");
        updateDto.setRecommended(false);

        given().spec(authAsOwner())
                .body(updateDto)
                .when().put()
                .then().statusCode(200)
                .body("rating", equalTo(3))
                .body("recommended", equalTo(false));
    }

    @Test
    public void updateReview_AsOtherUser_ThrowsUnauthorized() {
        ReviewDTO updateDto = new ReviewDTO();
        updateDto.setId(testReview.getId());
        updateDto.setRating(1);
        updateDto.setText("Hacked review!");

        // otherUser tries to edit reviewOwner's review
        given().spec(authAsOtherUser())
                .body(updateDto)
                .when().put()
                .then().statusCode(401);
    }

    // ==========================================
    // DELETION SECURITY & BUSINESS LOGIC
    // ==========================================

    @Test
    public void deleteReview_AsCreator_DeletesReview() {
        given().spec(authAsOwner())
                .pathParam("id", testReview.getId())
                .when().delete("/{id}")
                .then().statusCode(200);
    }

    @Test
    public void deleteReview_AsAdmin_DeletesReview() {
        given().spec(authAsAdmin()) // Admins have bypass clearance
                .pathParam("id", testReview.getId())
                .when().delete("/{id}")
                .then().statusCode(200);
    }

    @Test
    public void deleteReview_AsOtherUser_ThrowsUnauthorized() {
        // otherUser tries to delete reviewOwner's review
        given().spec(authAsOtherUser())
                .pathParam("id", testReview.getId())
                .when().delete("/{id}")
                .then().statusCode(401); //
    }


    // ==========================================
    // AUTHENTICATION HELPERS
    // ==========================================

    private String loginAndGetCookie(String username, String password) {
        return given().contentType(ContentType.JSON).body(new LoginRequest(username, password))
                .when().post(BASE_URL_AUTH + "/login").getCookie(JWT_COOKIE_NAME);
    }

    private RequestSpecification authAsAdmin() {
        return new RequestSpecBuilder().setBasePath(BASE_URL_REVIEWS).setContentType(ContentType.JSON)
                .addCookie(JWT_COOKIE_NAME, adminCookie).build();
    }

    private RequestSpecification authAsOwner() {
        return new RequestSpecBuilder().setBasePath(BASE_URL_REVIEWS).setContentType(ContentType.JSON)
                .addCookie(JWT_COOKIE_NAME, ownerCookie).build();
    }

    private RequestSpecification authAsOtherUser() {
        return new RequestSpecBuilder().setBasePath(BASE_URL_REVIEWS).setContentType(ContentType.JSON)
                .addCookie(JWT_COOKIE_NAME, otherUserCookie).build();
    }
}