package com.tfg.backend.api;

import com.tfg.backend.BackendApplication;
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

import java.time.YearMonth;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(
        classes = BackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
public class OrderApiFunctionalITest {

    @LocalServerPort
    int port;

    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ShopRepository shopRepository;
    @Autowired private ShopStockRepository shopStockRepository;
    @Autowired private TruckRepository truckRepository;

    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private EntityManager entityManager;

    private static final String JWT_COOKIE_NAME = "AuthToken";
    private static final String BASE_URL_AUTH = "/api/v1/auth";
    private static final String BASE_URL_ORDERS = "/api/v1/orders";

    private User testAdmin;
    private User testUser;
    private User hackerUser;
    private Product testProduct;
    private Shop testShop;
    private Truck testTruck;

    private Long addressId;
    private Long cardId;

    // Cached cookies to avoid repeated login requests
    private String adminCookie;
    private String userCookie;
    private String hackerCookie;

    @BeforeEach
    public void setUp() {
        RestAssured.reset();
        RestAssured.baseURI = "https://localhost:" + port;
        RestAssured.useRelaxedHTTPSValidation();

        // 1. BYPASS SECURITY FOR CLEANUP
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "pass",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );

        cleanDatabase();
        SecurityContextHolder.clearContext();

        // 2. DATA CREATION (Wrapped in a transaction)
        transactionTemplate.executeWithoutResult(status -> {

            testProduct = new Product("Test Product", "Desc", 10.0, 5.0);
            testProduct.setReferenceCode("REF-1234");
            testProduct = productRepository.saveAndFlush(testProduct);

            testShop = new Shop("Test Shop", new Address("Shop", "Shop St", "1", "1", "00000", "City", "Country"), 5000.0);
            testShop = shopRepository.saveAndFlush(testShop);

            // ADMIN CREATION (Has permission to change order status and assign trucks)
            testAdmin = new User("Admin", "admin_ord", "admin@test.com", passwordEncoder.encode("pass"), "ADMIN");
            testAdmin = userRepository.saveAndFlush(testAdmin);

            ShopStock stock = new ShopStock(testShop, testProduct, 5);
            shopStockRepository.saveAndFlush(stock);

            // TRUCK CREATION
            Address truckAddress = new Address("Garage", "Ind St", "1", "1", "28001", "City", "Country");
            testTruck = new Truck("1111-ORD", truckAddress, 20);
            testTruck = truckRepository.saveAndFlush(testTruck);

            testTruck.setAssignedShop(testShop);
            testTruck = truckRepository.saveAndFlush(testTruck);

            // BUYER USER CREATION
            testUser = new User("Buyer User", "buyer_user", "buyer@test.com", passwordEncoder.encode("pass"), "USER");
            testUser.setSelectedShop(testShop);

            Address address = new Address("Home", "Fake St", "123", "1A", "00000", "City", "Country");
            testUser.getAddresses().add(address);

            PaymentCard card = new PaymentCard("My Card", "Buyer User", "1111222233334444", "123", YearMonth.now().plusYears(1));
            testUser.getCards().add(card);

            testUser = userRepository.saveAndFlush(testUser);

            addressId = testUser.getAddresses().iterator().next().getId();
            cardId = testUser.getCards().iterator().next().getId();

            // MALICIOUS USER CREATION
            hackerUser = new User("Hacker", "hacker", "hacker@test.com", passwordEncoder.encode("pass"), "USER");
            hackerUser = userRepository.saveAndFlush(hackerUser);

            entityManager.flush();
        });

        // 3. CACHE LOGIN COOKIES
        adminCookie = loginAndGetCookie(testAdmin.getUsername(), "pass");
        userCookie = loginAndGetCookie(testUser.getUsername(), "pass");
        hackerCookie = loginAndGetCookie(hackerUser.getUsername(), "pass");
    }

    @AfterEach
    public void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        transactionTemplate.executeWithoutResult(status -> {
            entityManager.clear();

            entityManager.createQuery("DELETE FROM OrderItem").executeUpdate();
            entityManager.createQuery("DELETE FROM Order").executeUpdate();
            shopStockRepository.deleteAll();

            entityManager.createQuery("UPDATE Truck t SET t.assignedDriver = null, t.assignedShop = null").executeUpdate();
            entityManager.createQuery("UPDATE Shop s SET s.assignedManager = null").executeUpdate();

            entityManager.flush();
            entityManager.clear();

            userRepository.findAll().forEach(u -> {
                u.setSelectedShop(null);
                u.getFavouriteProducts().clear();
                userRepository.save(u);
            });
            userRepository.flush();

            shopRepository.findAll().forEach(s -> {
                s.setAssignedManager(null);
                shopRepository.save(s);
            });
            shopRepository.flush();

            truckRepository.deleteAll();
            productRepository.deleteAll();
            userRepository.deleteAll();
            shopRepository.deleteAll();
            entityManager.createQuery("DELETE FROM Address").executeUpdate();

            entityManager.flush();
            entityManager.clear();
        });
    }

    // --- AUTHENTICATION HELPERS ---

    private String loginAndGetCookie(String username, String password) {
        return given().contentType(ContentType.JSON).body(new LoginRequest(username, password))
                .when().post(BASE_URL_AUTH + "/login").getCookie(JWT_COOKIE_NAME);
    }

    private RequestSpecification authAsAdmin() { return new RequestSpecBuilder().setBasePath(BASE_URL_ORDERS).setContentType(ContentType.JSON).addCookie(JWT_COOKIE_NAME, adminCookie).build(); }
    private RequestSpecification authAsUser() { return new RequestSpecBuilder().setBasePath(BASE_URL_ORDERS).setContentType(ContentType.JSON).addCookie(JWT_COOKIE_NAME, userCookie).build(); }
    private RequestSpecification authAsHacker() { return new RequestSpecBuilder().setBasePath(BASE_URL_ORDERS).setContentType(ContentType.JSON).addCookie(JWT_COOKIE_NAME, hackerCookie).build(); }

    // ==========================================
    // CART TESTS
    // ==========================================

    @Test
    public void updateItemQuantity_InCart_UpdatesCorrectly() {
        given().spec(authAsUser()).pathParam("id", testProduct.getId()).queryParam("quantity", 1).when().post("/cart/{id}");

        given().spec(authAsUser())
                .pathParam("id", testProduct.getId())
                .queryParam("quantity", 3)
                .when().put("/cart/{id}")
                .then().statusCode(200);

        given().spec(authAsUser())
                .when().get("/cart/summary")
                .then().statusCode(200)
                .body("totalItems", equalTo(3))
                .body("totalCost", equalTo(30.0f));
    }

    @Test
    public void clearCartItems_RemovesAllItems() {
        given().spec(authAsUser()).pathParam("id", testProduct.getId()).queryParam("quantity", 2).when().post("/cart/{id}");

        given().spec(authAsUser())
                .when().delete("/cart")
                .then().statusCode(200)
                .body("totalItems", equalTo(0));
    }

    @Test
    public void cartLifecycleTest() {
        Long orderItemId = given().spec(authAsUser())
                .pathParam("id", testProduct.getId())
                .queryParam("quantity", 2)
                .when().post("/cart/{id}")
                .then().statusCode(201).extract().jsonPath().getLong("id");

        given().spec(authAsUser())
                .when().get("/cart/summary")
                .then().statusCode(200).body("totalItems", equalTo(2)).body("totalCost", equalTo(20.0f));

        given().spec(authAsUser())
                .pathParam("itemId", orderItemId)
                .when().delete("/cart/{itemId}")
                .then().statusCode(200).body("totalItems", equalTo(0));
    }

    @Test
    public void addItemToCart_ExceedsStock_ThrowsMethodNotAllowed() {
        given().spec(authAsUser())
                .pathParam("id", testProduct.getId())
                .queryParam("quantity", 6)
                .when().post("/cart/{id}")
                .then().statusCode(405);
    }

    // ==========================================
    // ORDER TESTS
    // ==========================================

    @Test
    public void createOrderTest() {
        given().spec(authAsUser()).pathParam("id", testProduct.getId()).queryParam("quantity", 1).when().post("/cart/{id}");

        given().spec(authAsUser())
                .queryParam("addressId", addressId)
                .queryParam("cardId", cardId)
                .when().post() // Uses POST to base URL
                .then().statusCode(201).body("totalCost", equalTo(10.0f));
    }

    @Test
    public void cancelOrder_ForbiddenForOtherUsers() {
        given().spec(authAsUser()).pathParam("id", testProduct.getId()).queryParam("quantity", 1).when().post("/cart/{id}");
        Long orderId = given().spec(authAsUser()).queryParam("addressId", addressId).queryParam("cardId", cardId)
                .when().post().jsonPath().getLong("id");

        given().spec(authAsHacker())
                .pathParam("id", orderId)
                .when().put("/cancel/{id}")
                .then().statusCode(403);
    }

    @Test
    public void getOrdersPage_AsUser_ReturnsList() {
        given().spec(authAsUser()).pathParam("id", testProduct.getId()).queryParam("quantity", 1).when().post("/cart/{id}");
        given().spec(authAsUser()).queryParam("addressId", addressId).queryParam("cardId", cardId).when().post();

        given().spec(authAsUser())
                .when().get()
                .then().statusCode(200)
                .body("items", notNullValue())
                .body("items.size()", greaterThan(0));
    }

    @Test
    public void assignTruckToOrder_AsAdmin_UpdatesAssignment() {
        given().spec(authAsUser()).pathParam("id", testProduct.getId()).queryParam("quantity", 1).when().post("/cart/{id}");
        Long orderId = given().spec(authAsUser()).queryParam("addressId", addressId).queryParam("cardId", cardId)
                .when().post().jsonPath().getLong("id");

        given().spec(authAsAdmin())
                .pathParam("orderId", orderId)
                .pathParam("truckId", testTruck.getId())
                .queryParam("state", true)
                .when().post("/{orderId}/assign/truck/{truckId}")
                .then().statusCode(200)
                .body("assignedTruckId", equalTo(testTruck.getId().intValue()));
    }

    @Test
    public void updateOrderStatus_AsAdmin_ChangesStatusSuccessfully() {
        given().spec(authAsUser()).pathParam("id", testProduct.getId()).queryParam("quantity", 1).when().post("/cart/{id}");
        Long orderId = given().spec(authAsUser()).queryParam("addressId", addressId).queryParam("cardId", cardId)
                .when().post().jsonPath().getLong("id");

        given().spec(authAsAdmin())
                .pathParam("id", orderId)
                .queryParam("orderStatus", "SENT")
                .queryParam("comment", "Order has been dispatched")
                .when().put("/{id}")
                .then().statusCode(200)
                .body("history.size()", greaterThan(0))
                .body("history[-1].status", equalTo("Enviado"))
                .body("history[-1].updates[-1].description", equalTo("Order has been dispatched"));
    }
}