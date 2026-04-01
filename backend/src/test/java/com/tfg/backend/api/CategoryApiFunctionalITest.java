package com.tfg.backend.api;

import com.tfg.backend.BackendApplication;
import com.tfg.backend.dto.CategoryDTO;
import com.tfg.backend.model.Category;
import com.tfg.backend.model.User;
import com.tfg.backend.repository.CategoryRepository;
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
public class CategoryApiFunctionalITest {

    @LocalServerPort
    int port;

    @Autowired private CategoryRepository categoryRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private EntityManager entityManager;

    private static final String JWT_COOKIE_NAME = "AuthToken";
    private static final String BASE_URL_AUTH = "/api/v1/auth";
    private static final String BASE_URL_CATEGORIES = "/api/v1/categories";

    private User testAdmin;
    private Category otrosCategory;
    private String adminCookie;

    @BeforeEach
    public void setUp() {
        RestAssured.reset();
        RestAssured.baseURI = "https://localhost:" + port;
        RestAssured.useRelaxedHTTPSValidation();

        // 1. Admin bypass cleanup (the only who has permission to delete categories and users)
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "pass",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );
        cleanDatabase();
        SecurityContextHolder.clearContext();

        // 2. Data provisioning
        transactionTemplate.executeWithoutResult(status -> {
            // "Otros" is mandatory for system integrity
            otrosCategory = new Category("Otros", "icon", "banner", "Default", "Default");
            otrosCategory = categoryRepository.saveAndFlush(otrosCategory);

            testAdmin = new User("Admin", "admin_cat", "admin@cat.com", passwordEncoder.encode("pass"), "ADMIN");
            userRepository.saveAndFlush(testAdmin);
        });

        // 3. Cache login
        adminCookie = given().contentType(ContentType.JSON).body(new LoginRequest(testAdmin.getUsername(), "pass"))
                .when().post(BASE_URL_AUTH + "/login").getCookie(JWT_COOKIE_NAME);
    }

    @AfterEach
    public void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        transactionTemplate.executeWithoutResult(status -> {
            categoryRepository.deleteAll();
            userRepository.deleteAll();
            entityManager.flush();
            entityManager.clear();
        });
    }

    private RequestSpecification authAsAdmin() {
        return new RequestSpecBuilder().setBasePath(BASE_URL_CATEGORIES).setContentType(ContentType.JSON)
                .addCookie(JWT_COOKIE_NAME, adminCookie).build();
    }

    private RequestSpecification asAnonymous() {
        return new RequestSpecBuilder().setBasePath(BASE_URL_CATEGORIES).setContentType(ContentType.JSON).build();
    }

    // ==========================================
    // READ ENDPOINTS
    // ==========================================

    @Test
    public void getAllCategories_ReturnsPagedAndListFormats() {
        // Test Paged Response
        given().spec(asAnonymous())
                .when().get("/")
                .then().statusCode(200)
                .body("items", notNullValue());

        // Test List Response
        given().spec(asAnonymous())
                .when().get("/list")
                .then().statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("name", hasItem("Otros"));
    }

    // ==========================================
    // IMAGE MANAGEMENT TESTS
    // ==========================================

    @Test
    public void uploadCategoryImage_Admin_UploadsMultipart() {
        // Create a category to upload an image to it
        CategoryDTO dto = new CategoryDTO();
        dto.setName("Image Category");
        Long catId = given().spec(authAsAdmin()).body(dto).when().post().jsonPath().getLong("id");

        given().spec(authAsAdmin())
                .contentType("multipart/form-data") // Override JSON content type
                .pathParam("id", catId)
                .multiPart("image", "cat.jpg", "fake_image".getBytes(), "image/jpeg")
                .when().post("/{id}/image")
                .then().statusCode(200)
                .body("imageInfo.imageUrl", notNullValue());
    }

    @Test
    public void deleteCategoryImage_Admin_RemovesImage() {
        given().spec(authAsAdmin())
                .pathParam("id", otrosCategory.getId())
                .when().delete("/{id}/image")
                .then().statusCode(200)
                .body("imageInfo", notNullValue());
    }

    // ==========================================
    // BUSINESS LOGIC & CONSTRAINTS
    // ==========================================

    @Test
    public void categoryForbiddenOperations_ProtectOtrosCategory() {
        // 1. Ask the API for the real category named "Otros" to get its actual ID
        Long realOtrosId = given().spec(asAnonymous())
                .pathParam("name", "Otros")
                .when().get("/name/{name}")
                .then().statusCode(200)
                .extract().jsonPath().getLong("id");

        // 2. Attempt to Update: Must return 403
        CategoryDTO updateDto = new CategoryDTO();
        updateDto.setName("Illegal Change");
        given().spec(authAsAdmin())
                .pathParam("id", realOtrosId)
                .body(updateDto)
                .when().put("/{id}")
                .then().statusCode(403);

        // 3. Attempt to Delete: Must return 403
        given().spec(authAsAdmin())
                .pathParam("id", realOtrosId)
                .when().delete("/{id}")
                .then().statusCode(403);
    }

    @Test
    public void circularReference_ForbiddenTest() {
        // Create Hierarchy: A -> B
        CategoryDTO parentDto = new CategoryDTO();
        parentDto.setName("Parent A");
        Long parentId = given().spec(authAsAdmin()).body(parentDto).when().post().jsonPath().getLong("id");

        CategoryDTO childDto = new CategoryDTO();
        childDto.setName("Child B");
        childDto.setParentId(parentId);
        Long childId = given().spec(authAsAdmin()).body(childDto).when().post().jsonPath().getLong("id");

        // Attempt Circularity: Set A's parent as its own child B
        parentDto.setId(parentId);
        parentDto.setParentId(childId);

        given().spec(authAsAdmin())
                .pathParam("id", parentId)
                .body(parentDto)
                .when().put("/{id}")
                .then().statusCode(403);
    }

    @Test
    public void createSubCategory_LinksCorrectParent() {
        CategoryDTO childDto = new CategoryDTO();
        childDto.setName("Subcategory");
        childDto.setParentId(otrosCategory.getId());

        given().spec(authAsAdmin())
                .body(childDto)
                .when().post()
                .then().statusCode(201)
                .body("parentId", equalTo(otrosCategory.getId().intValue()));
    }
}