package com.tfg.backend.api;

import com.tfg.backend.BackendApplication;
import com.tfg.backend.dto.AddressDTO;
import com.tfg.backend.dto.ShopDTO;
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
public class ShopApiFunctionalITest {

    @LocalServerPort
    int port;

    @Autowired private UserRepository userRepository;
    @Autowired private ShopRepository shopRepository;
    @Autowired private ShopStockRepository shopStockRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private TruckRepository truckRepository;

    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private EntityManager entityManager;

    private static final String JWT_COOKIE_NAME = "AuthToken";
    private static final String BASE_URL_AUTH = "/api/v1/auth";
    private static final String BASE_URL_SHOPS = "/api/v1/shops";

    private User testAdmin;
    private User testManager;
    private User testDriver;
    private User testUser;

    private Shop testShop;
    private Truck testTruck;
    private ShopStock testStock;
    private Product testProduct;

    private String adminCookie;
    private String managerCookie;
    private String driverCookie;
    private String userCookie;

    @BeforeEach
    public void setUp() {
        RestAssured.reset();
        RestAssured.baseURI = "https://localhost:" + port;
        RestAssured.useRelaxedHTTPSValidation();

        // 1. Admin bypass DB cleaning
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "pass",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );

        cleanDatabase();
        SecurityContextHolder.clearContext();

        // 2. Data creation
        Category category = new Category("Otros", "icon", "banner", "Desc", "Desc");
        categoryRepository.saveAndFlush(category);

        testProduct = new Product("API Shop Product", "Desc", 10.0, 5.0);
        testProduct.setReferenceCode("REF-SHP-PROD");
        testProduct.setActive(true);
        testProduct = productRepository.saveAndFlush(testProduct);

        // Shop creation
        Address shopAddress = new Address("ShopHQ", "Main Street", "1", "1A", "28000", "Madrid", "Spain");
        testShop = new Shop("API Test Shop", shopAddress, 5000.0);
        testShop.setReferenceCode("SHP-001");
        testShop = shopRepository.saveAndFlush(testShop);

        // Stock and users creation
        testStock = new ShopStock(testShop, testProduct, 50);
        testStock = shopStockRepository.saveAndFlush(testStock);

        testAdmin = new User("Admin", "admin_shp", "admin@shp.com", passwordEncoder.encode("pass"), "ADMIN");
        userRepository.saveAndFlush(testAdmin);

        testManager = new User("Manager", "manager_shp", "manager@shp.com", passwordEncoder.encode("pass"), "MANAGER");
        userRepository.saveAndFlush(testManager);

        testDriver = new User("Driver", "driver_shp", "driver@shp.com", passwordEncoder.encode("pass"), "DRIVER");
        userRepository.saveAndFlush(testDriver);

        testUser = new User("User", "user_shp", "user@shp.com", passwordEncoder.encode("pass"), "USER");
        userRepository.saveAndFlush(testUser);

        // User-Shop Assignments
        testShop.setAssignedManager(testManager);
        testShop = shopRepository.saveAndFlush(testShop);


        Address truckAddress = new Address("Garage", "Industrial St", "10", "B", "28001", "Madrid", "Spain");
        testTruck = new Truck("1234-ABC", truckAddress, 25);
        testTruck = truckRepository.saveAndFlush(testTruck);
        testTruck.setAssignedDriver(testDriver);
        testTruck.setAssignedShop(testShop);
        testTruck = truckRepository.saveAndFlush(testTruck);

        // Cache login cookies
        adminCookie = loginAndGetCookie(testAdmin.getUsername(), "pass");
        managerCookie = loginAndGetCookie(testManager.getUsername(), "pass");
        driverCookie = loginAndGetCookie(testDriver.getUsername(), "pass");
        userCookie = loginAndGetCookie(testUser.getUsername(), "pass");
    }

    @AfterEach
    public void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        transactionTemplate.executeWithoutResult(status -> {
            // Preventive dependency deletion
            entityManager.createQuery("DELETE FROM OrderItem").executeUpdate();
            entityManager.createQuery("DELETE FROM Order").executeUpdate();
            shopStockRepository.deleteAll();

            // Unlink ManyToMany
            userRepository.findAll().forEach(u -> {
                u.getFavouriteProducts().clear();
                userRepository.save(u);
            });
            userRepository.flush();

            // Break FKs in DB to avoid data integrity errors
            entityManager.createQuery("UPDATE Truck t SET t.assignedDriver = null, t.assignedShop = null").executeUpdate();
            entityManager.createQuery("UPDATE Shop s SET s.assignedManager = null").executeUpdate();
            entityManager.createQuery("UPDATE User u SET u.selectedShop = null").executeUpdate();

            // Empty Hibernate memory
            entityManager.flush();
            entityManager.clear();

            // Clean and secure entity deletions
            truckRepository.deleteAll();
            shopRepository.deleteAll();
            userRepository.deleteAll();
            productRepository.deleteAll();
            categoryRepository.deleteAll();

            entityManager.createQuery("DELETE FROM Address").executeUpdate();
        });
    }

    // ==========================================
    // READ ENDPOINTS TESTS
    // ==========================================

    @Test
    public void getAllShopsPage_AsAdmin_ReturnsPagedShops() {
        given().spec(authAsAdmin())
                .when().get("/")
                .then().statusCode(200)
                .body("items", notNullValue())
                .body("items.size()", greaterThan(0));
    }

    @Test
    public void getAssignedShopsPage_AsManager_ReturnsPagedShops() {
        given().spec(authAsManager())
                .when().get()
                .then().statusCode(200)
                .body("items", notNullValue())
                .body("items[0].id", equalTo(testShop.getId().intValue()));
    }

    @Test
    public void getAllShopsList_AsUser_ReturnsList() {
        given().spec(authAsUser())
                .when().get("/list")
                .then().statusCode(200)
                .body("size()", greaterThan(0))
                .body("[0].name", equalTo("API Test Shop"))
                .body("[0].address", notNullValue());
    }

    @Test
    public void getShopByAssignedTruckId_AsDriver_ReturnsShop() {
        given().spec(authAsDriver())
                .pathParam("id", testTruck.getId())
                .when().get("/truck/{id}")
                .then().statusCode(200)
                .body("id", equalTo(testShop.getId().intValue()));
    }

    @Test
    public void getShopStocks_AsManager_ReturnsPagedStocks() {
        given().spec(authAsManager())
                .pathParam("id", testShop.getId())
                .when().get("/stock/{id}")
                .then().statusCode(200)
                .body("items.size()", greaterThan(0))
                .body("items[0].units", equalTo(50));
    }

    // ==========================================
    // WRITE ENDPOINTS TESTS (CRUD & Assignments)
    // ==========================================

    @Test
    public void createShop_AsAdmin_CreatesShopWithAddress() {
        ShopDTO newShop = new ShopDTO();
        newShop.setName("New Madrid Shop");
        newShop.setReferenceCode("SHP-MAD-01");
        newShop.setAssignedBudget(10000.0);

        newShop.setAddress(new AddressDTO(new Address("Alias", "New Street", "2", "2", "28000", "Madrid", "Spain")));

        given().spec(authAsAdmin())
                .body(newShop)
                .when().post()
                .then().statusCode(201)
                .header("Location", notNullValue())
                .body("name", equalTo("New Madrid Shop"))
                .body("address.street", equalTo("New Street"));
    }

    @Test
    public void updateShop_AsManager_UpdatesShopAndAddress() {
        ShopDTO updateShop = new ShopDTO();
        updateShop.setName("Updated Shop Name");
        updateShop.setAssignedBudget(7500.0);

        updateShop.setAddress(new AddressDTO(new Address("Alias", "Updated St", "3", "3", "28000", "Madrid", "Spain")));

        given().spec(authAsManager())
                .pathParam("id", testShop.getId())
                .body(updateShop)
                .when().put("/{id}")
                .then().statusCode(202)
                .body("name", equalTo("Updated Shop Name"))
                .body("address.street", equalTo("Updated St"));
    }

    @Test
    public void setAssignedTruck_AsManager_UpdatesAssignment() {
        given().spec(authAsManager())
                .pathParam("shopId", testShop.getId())
                .pathParam("truckId", testTruck.getId())
                .queryParam("state", false)
                .when().put("/{shopId}/assign/truck/{truckId}")
                .then().statusCode(200);

        given().spec(authAsManager())
                .pathParam("shopId", testShop.getId())
                .pathParam("truckId", testTruck.getId())
                .queryParam("state", true)
                .when().put("/{shopId}/assign/truck/{truckId}")
                .then().statusCode(200)
                .body("plateNumber", equalTo("1234-ABC"));
    }

    @Test
    public void setAssignedManager_AsAdmin_UpdatesAssignment() {
        given().spec(authAsAdmin())
                .pathParam("shopId", testShop.getId())
                .pathParam("userId", testUser.getId())
                .queryParam("state", true)
                .when().put("/{shopId}/assign/manager/{userId}")
                .then().statusCode(200)
                .body("assignedManager.id", equalTo(testUser.getId().intValue()));
    }

    @Test
    public void restockProduct_AsManager_AddsUnitsToStock() {
        given().spec(authAsManager())
                .pathParam("stockId", testStock.getId())
                .queryParam("units", 25)
                .when().put("/restock/{stockId}")
                .then().statusCode(200)
                .body("units", equalTo(75));
    }

    @Test
    public void toggleLocalActivation_AsManager_ChangesStockState() {
        given().spec(authAsManager())
                .pathParam("id", testStock.getId())
                .queryParam("state", false)
                .when().put("/active/{id}")
                .then().statusCode(200);
    }

    @Test
    public void deleteShop_AsAdmin_DeletesShop() {
        given().spec(authAsAdmin())
                .pathParam("id", testShop.getId())
                .when().delete("/{id}")
                .then().statusCode(200);
    }

    // ==========================================
    // MULTIPART IMAGE TESTS
    // ==========================================

    @Test
    public void uploadShopImage_AsAdmin_UploadsMultipart() {
        given().spec(authAsAdmin())
                .contentType("multipart/form-data")
                .pathParam("id", testShop.getId())
                .multiPart("image", "shop_front.jpg", "fake_image_content".getBytes(), "image/jpeg")
                .when().put("/image/{id}")
                .then().statusCode(200)
                .body("imageInfo", notNullValue());
    }


    // ==========================================
    // AUTHENTICATION HELPERS
    // ==========================================

    private String loginAndGetCookie(String username, String password) {
        return given().contentType(ContentType.JSON).body(new LoginRequest(username, password))
                .when().post(BASE_URL_AUTH + "/login").getCookie(JWT_COOKIE_NAME);
    }

    private RequestSpecification authAsAdmin() { return new RequestSpecBuilder().setBasePath(BASE_URL_SHOPS).setContentType(ContentType.JSON).addCookie(JWT_COOKIE_NAME, adminCookie).build(); }
    private RequestSpecification authAsManager() { return new RequestSpecBuilder().setBasePath(BASE_URL_SHOPS).setContentType(ContentType.JSON).addCookie(JWT_COOKIE_NAME, managerCookie).build(); }
    private RequestSpecification authAsDriver() { return new RequestSpecBuilder().setBasePath(BASE_URL_SHOPS).setContentType(ContentType.JSON).addCookie(JWT_COOKIE_NAME, driverCookie).build(); }
    private RequestSpecification authAsUser() { return new RequestSpecBuilder().setBasePath(BASE_URL_SHOPS).setContentType(ContentType.JSON).addCookie(JWT_COOKIE_NAME, userCookie).build(); }
}