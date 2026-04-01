package com.tfg.backend.api;

import com.tfg.backend.BackendApplication;
import com.tfg.backend.model.*;
import com.tfg.backend.repository.*;
import com.tfg.backend.security.jwt.LoginRequest;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.YearMonth;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(
        classes = BackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test") // 1. CRÍTICO: Entorno aislado
public class OrderApiFunctionalITest {

    @LocalServerPort
    int port;

    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ShopRepository shopRepository;
    @Autowired private ShopStockRepository shopStockRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private static final String JWT_COOKIE_NAME = "AuthToken";
    private static final String BASE_URL_AUTH = "/api/v1/auth";
    private static final String BASE_URL_ORDERS = "/api/v1/orders";

    private User testUser;
    private User hackerUser;
    private Product testProduct;
    private Shop testShop;
    private Long addressId;
    private Long cardId;

    @BeforeEach
    public void setUp() {
        RestAssured.reset();
        RestAssured.baseURI = "https://localhost:" + port;
        RestAssured.useRelaxedHTTPSValidation();

        // 2. CREACIÓN DEL SHOP Y PRODUCTO
        testProduct = new Product("Test Product", "Desc", 10.0, 5.0);
        testProduct.setReferenceCode("REF-1234");
        testProduct = productRepository.save(testProduct);

        testShop = new Shop("Test Shop", new Address("Shop", "Shop St", "1", "1", "00000", "City", "Country"), 5000.0);
        testShop = shopRepository.save(testShop);

        ShopStock stock = new ShopStock(testShop, testProduct, 5); // OJO: Solo hay 5 unidades en stock
        shopStockRepository.save(stock);

        // 3. CREACIÓN DEL USUARIO COMPRADOR
        testUser = new User("Buyer User", "buyer_user", "buyer@test.com", passwordEncoder.encode("pass"), "USER");
        testUser.setSelectedShop(testShop);

        Address address = new Address("Home", "Fake St", "123", "1A", "00000", "City", "Country");
        testUser.getAddresses().add(address);

        PaymentCard card = new PaymentCard("My Card", "Buyer User", "1111222233334444", "123", YearMonth.now().plusYears(1));
        testUser.getCards().add(card);

        testUser = userRepository.save(testUser);

        // Guardamos los IDs para usarlos en el test sin tener que hacer recargas raras ni Thread.sleep()
        addressId = testUser.getAddresses().getFirst().getId();
        cardId = testUser.getCards().getFirst().getId();

        // 4. CREACIÓN DE UN USUARIO MALICIOSO (Para probar seguridad)
        hackerUser = new User("Hacker", "hacker", "hacker@test.com", passwordEncoder.encode("pass"), "USER");
        userRepository.save(hackerUser);

        // 5. LOGIN DEL USUARIO COMPRADOR
        Response loginResponse = given()
                .contentType(ContentType.JSON)
                .body(new LoginRequest(testUser.getUsername(), "pass"))
                .when()
                .post(BASE_URL_AUTH + "/login");

        RestAssured.requestSpecification = new RequestSpecBuilder()
                .setBasePath(BASE_URL_ORDERS)
                .setContentType(ContentType.JSON)
                .addCookie(JWT_COOKIE_NAME, loginResponse.getCookie(JWT_COOKIE_NAME))
                .build();
    }

    @AfterEach
    public void tearDown() {
        orderRepository.deleteAll();
        shopStockRepository.deleteAll();
        userRepository.deleteAll();
        shopRepository.deleteAll();
        productRepository.deleteAll();
    }

    // --- TESTS DE CARRITO ---

    @Test
    public void cartLifecycleTest() {
        // 1. Add item
        given()
                .pathParam("id", testProduct.getId())
                .queryParam("quantity", 2)
                .when()
                .post("/cart/{id}")
                .then()
                .statusCode(201);

        // 2. Summary
        given()
                .when()
                .get("/cart/summary")
                .then()
                .statusCode(200)
                .body("totalItems", equalTo(2))
                .body("totalCost", equalTo(20.0f)); // 2 * 10.0

        // 3. Delete item
        given()
                .pathParam("id", testProduct.getId())
                .when()
                .delete("/cart/{id}")
                .then()
                .statusCode(200)
                .body("totalItems", equalTo(0));
    }

    @Test
    public void addItemToCart_ExceedsStock_ThrowsMethodNotAllowed() {
        // Stock is 5. We try to add 6.
        given()
                .pathParam("id", testProduct.getId())
                .queryParam("quantity", 6)
                .when()
                .post("/cart/{id}")
                .then()
                .statusCode(405); // METHOD_NOT_ALLOWED
    }

    // --- TESTS DE PEDIDOS ---

    @Test
    public void createOrderTest() {
        // 1. Prepare cart
        given().pathParam("id", testProduct.getId()).queryParam("quantity", 1)
                .when().post("/cart/{id}").then().statusCode(201);

        // 2. Create order (Sin Thread.sleep gracias a que los IDs ya están guardados)
        given()
                .queryParam("addressId", addressId)
                .queryParam("cardId", cardId)
                .when()
                .post()
                .then()
                .statusCode(201)
                // Shipping does not count
                .body("totalCost", equalTo(10.0f));
    }

    @Test
    public void cancelOrder_ForbiddenForOtherUsers() {
        given().pathParam("id", testProduct.getId()).queryParam("quantity", 1).when().post("/cart/{id}");
        Long orderId = given().queryParam("addressId", addressId).queryParam("cardId", cardId)
                .when().post().jsonPath().getLong("id");

        // Remove cookie from the user that purchased the product
        RestAssured.requestSpecification = null;

        // Log in as another user
        Response hackerLogin = given()
                .contentType(ContentType.JSON)
                .body(new LoginRequest(hackerUser.getUsername(), "pass"))
                .when().post(BASE_URL_AUTH + "/login");

        // Try to cancel the order
        given()
                .basePath(BASE_URL_ORDERS)
                .contentType(ContentType.JSON)
                .cookie(JWT_COOKIE_NAME, hackerLogin.getCookie(JWT_COOKIE_NAME))
                .pathParam("id", orderId)
                .when()
                .put("/cancel/{id}")
                .then()
                .statusCode(403);
    }
}