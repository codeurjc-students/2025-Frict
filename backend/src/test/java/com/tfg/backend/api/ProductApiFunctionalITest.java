package com.tfg.backend.api;

import com.tfg.backend.BackendApplication;
import com.tfg.backend.dto.ProductDTO;
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
public class ProductApiFunctionalITest {

    @LocalServerPort
    int port;

    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ShopRepository shopRepository;
    @Autowired private ShopStockRepository shopStockRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;

    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private EntityManager entityManager;

    private static final String JWT_COOKIE_NAME = "AuthToken";
    private static final String BASE_URL_AUTH = "/api/v1/auth";
    private static final String BASE_URL_PRODUCTS = "/api/v1/products";

    private User testAdmin;
    private User testUser;
    private User testManager;
    private Product testProduct;
    private Shop testShop;

    // Cached cookies to avoid repeated login requests in the same test
    private String adminCookie;
    private String userCookie;
    private String managerCookie;

    @BeforeEach
    public void setUp() {
        RestAssured.reset();
        RestAssured.baseURI = "https://localhost:" + port;
        RestAssured.useRelaxedHTTPSValidation();

        // 1. DUPLICATE ENTRY FIX:
        // Authenticate manually as ADMIN in the SecurityContext so the ProductVisibilityAspect
        // disables the filters, allowing cleanDatabase() to see and delete INACTIVE products.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "pass",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );

        cleanDatabase();

        // Clear the dummy authentication before actual test setup
        SecurityContextHolder.clearContext();

        // 2. DATA CREATION (Using saveAndFlush to ensure immediate persistence)
        Category otrosCategory = new Category("Otros", "icon", "banner", "Desc", "Desc");
        categoryRepository.saveAndFlush(otrosCategory);

        testProduct = new Product("API Test Product", "Desc", 10.0, 5.0);
        testProduct.setReferenceCode("REF-API-123");
        testProduct.setActive(true);
        testProduct = productRepository.saveAndFlush(testProduct);

        testShop = new Shop("API Test Shop", new Address("Shop", "Shop St", "1", "1", "00000", "City", "Country"), 5000.0);
        testShop = shopRepository.saveAndFlush(testShop);

        shopStockRepository.saveAndFlush(new ShopStock(testShop, testProduct, 10));

        // Create test users
        testAdmin = new User("Admin", "admin_api", "admin@api.com", passwordEncoder.encode("pass"), "ADMIN");
        userRepository.saveAndFlush(testAdmin);

        testUser = new User("User", "user_api", "user@api.com", passwordEncoder.encode("pass"), "USER");
        testUser.setSelectedShop(testShop);
        userRepository.saveAndFlush(testUser);

        testManager = new User("Manager", "manager_api", "manager@api.com", passwordEncoder.encode("pass"), "MANAGER");
        userRepository.saveAndFlush(testManager);

        testShop.setAssignedManager(testManager);
        shopRepository.saveAndFlush(testShop);

        // 3. CACHE LOGIN COOKIES (Executed only once per test)
        adminCookie = loginAndGetCookie(testAdmin.getUsername(), "pass");
        userCookie = loginAndGetCookie(testUser.getUsername(), "pass");
        managerCookie = loginAndGetCookie(testManager.getUsername(), "pass");
    }

    @AfterEach
    public void tearDown() {
        cleanDatabase();
    }

    /**
     * Breaks ManyToMany and ManyToOne relationships before deleting records.
     * Prevents MySQL Foreign Key Constraints and Hibernate TransientObjectExceptions.
     */
    private void cleanDatabase() {
        transactionTemplate.executeWithoutResult(status -> {
            // Unlink and delete child entities first
            orderItemRepository.deleteAll();
            orderRepository.deleteAll();
            shopStockRepository.deleteAll();

            // Unlink relationships loaded in memory
            userRepository.findAll().forEach(u -> {
                u.setSelectedShop(null);
                u.getFavouriteProducts().clear();
                userRepository.save(u);
            });

            shopRepository.findAll().forEach(s -> {
                s.setAssignedManager(null);
                shopRepository.save(s);
            });

            userRepository.flush();
            shopRepository.flush();

            // Safely delete parent entities
            productRepository.deleteAll();
            categoryRepository.deleteAll();
            userRepository.deleteAll();
            shopRepository.deleteAll();

            // Force changes to apply before exiting the transaction
            entityManager.flush();
            entityManager.clear();
        });
    }

    // --- AUTHENTICATION HELPERS ---

    private String loginAndGetCookie(String username, String password) {
        return given().contentType(ContentType.JSON).body(new LoginRequest(username, password))
                .when().post(BASE_URL_AUTH + "/login").getCookie(JWT_COOKIE_NAME);
    }

    private RequestSpecification authAsAdmin() {
        return new RequestSpecBuilder().setBasePath(BASE_URL_PRODUCTS).setContentType(ContentType.JSON)
                .addCookie(JWT_COOKIE_NAME, adminCookie).build();
    }

    private RequestSpecification authAsUser() {
        return new RequestSpecBuilder().setBasePath(BASE_URL_PRODUCTS).setContentType(ContentType.JSON)
                .addCookie(JWT_COOKIE_NAME, userCookie).build();
    }

    private RequestSpecification authAsManager() {
        return new RequestSpecBuilder().setBasePath(BASE_URL_PRODUCTS).setContentType(ContentType.JSON)
                .addCookie(JWT_COOKIE_NAME, managerCookie).build();
    }

    private RequestSpecification asAnonymous() {
        return new RequestSpecBuilder().setBasePath(BASE_URL_PRODUCTS).setContentType(ContentType.JSON).build();
    }

    // ==========================================
    // BUSINESS LOGIC CRITICAL TESTS
    // ==========================================

    @Test
    public void getProduct_StockEnrichment_DependsOnRole() {
        given().spec(authAsUser())
                .pathParam("id", testProduct.getId())
                .when().get("/{id}")
                .then().statusCode(200)
                .body("availableUnits", equalTo(10));

        given().spec(asAnonymous())
                .pathParam("id", testProduct.getId())
                .when().get("/{id}")
                .then().statusCode(200)
                .body("availableUnits", equalTo(0));
    }

    @Test
    public void productVisibilityAspect_HidesInactiveProductsFromUsers() {
        // The admin deactivates the product
        given().spec(authAsAdmin())
                .pathParam("id", testProduct.getId()).queryParam("state", "false")
                .when().put("/active/{id}").then().statusCode(200);

        // An anon user should not see this product id in the results
        given().spec(asAnonymous())
                .when().get("/filter")
                .then().statusCode(200)
                .body("items.id", not(hasItem(testProduct.getId().intValue())));

        // The admin should keep seeing the product in their list
        given().spec(authAsAdmin())
                .when().get("/")
                .then().statusCode(200)
                .body("items.id", hasItem(testProduct.getId().intValue()));
    }

    // ==========================================
    // MULTIPART IMAGE TESTS (Real MinIO Upload)
    // ==========================================

    @Test
    public void updateProductImages_Admin_UploadsMultipartToMinio() {
        given().spec(authAsAdmin())
                // Override default JSON content-type from spec for file upload
                .contentType("multipart/form-data")
                .pathParam("id", testProduct.getId())
                .multiPart("existingImages", "[]", "application/json")
                .multiPart("newImages", "foto_test.jpg", "fake_image_content".getBytes(), "image/jpeg")
                .when()
                .put("/{id}/images")
                .then()
                .statusCode(200)
                .body("imagesInfo.size()", greaterThan(0))
                .body("imagesInfo[0].imageUrl", notNullValue())
                .body("imagesInfo[0].s3Key", notNullValue());
    }

    // ==========================================
    // STANDARD CRUD TESTS
    // ==========================================

    @Test
    public void getAllProducts_Admin_ReturnsPagedProducts() {
        given().spec(authAsAdmin())
                .when().get("/")
                .then().statusCode(200)
                .body("items", notNullValue());
    }

    @Test
    public void createProduct_Admin_CreatesAndReturnsProduct() {
        ProductDTO newProduct = new ProductDTO();
        newProduct.setName("New Admin Product");
        newProduct.setDescription("Created via API");
        newProduct.setReferenceCode("NEW-REF-001");
        newProduct.setSupplyPrice(15.0);
        newProduct.setCurrentPrice(25.0);
        newProduct.setActive(true);

        given().spec(authAsAdmin())
                .body(newProduct)
                .when().post()
                .then().statusCode(201)
                .body("name", equalTo("New Admin Product"));
    }

    @Test
    public void updateProduct_Admin_UpdatesProduct() {
        ProductDTO updateData = new ProductDTO();
        updateData.setName("Updated Product Name");
        updateData.setDescription(testProduct.getDescription());
        updateData.setReferenceCode("NEW-REF");
        updateData.setActive(true);
        updateData.setSupplyPrice(15.0);
        updateData.setCurrentPrice(25.0);

        given().spec(authAsAdmin())
                .pathParam("id", testProduct.getId())
                .body(updateData)
                .when().put("/{id}")
                .then().statusCode(200)
                .body("name", equalTo("Updated Product Name"));
    }

    @Test
    public void deleteProduct_Admin_DeletesProduct() {
        given().spec(authAsAdmin())
                .pathParam("id", testProduct.getId())
                .when().delete("/{id}")
                .then().statusCode(200);

        given().spec(asAnonymous())
                .pathParam("id", testProduct.getId())
                .when().get("/{id}")
                .then().statusCode(404);
    }
}