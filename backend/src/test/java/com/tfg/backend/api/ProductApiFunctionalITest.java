package com.tfg.backend.api;

import com.tfg.backend.BackendApplication;
import com.tfg.backend.model.User;
import com.tfg.backend.repository.UserRepository;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(
        classes = BackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class ProductApiFunctionalITest {

    @LocalServerPort
    int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // IMPORTANTE: Asegúrate de que coincide con el nombre de la cookie en tu AuthRestController
    private static final String JWT_COOKIE_NAME = "AuthToken";
    private static final String CONTENT_TYPE = "application/json";
    private static final String BASE_URL = "/api/v1";



    // Datos de prueba
    private String name = "Aspiradora inteligente";
    private String description = "Limpieza sin límites";
    private double currentPrice = 130.99;

    @BeforeEach
    public void setUp() {
        RestAssured.reset();
        RestAssured.baseURI = "https://localhost:" + port;
        RestAssured.useRelaxedHTTPSValidation();

        // Credenciales para el test
        String testUsername = "admin2";
        String testEmail = "admin2@test.com";
        String testPassword = "password123";

        // 1. Crear usuario ADMIN en la BD si NO existe
        // NOTA: Si tu repositorio no tiene 'existsByUsername', usa findByUsername(...).isEmpty()
        if (!userRepository.existsByUsername(testUsername)) {
            User adminUser = new User();

            // Requisitos obligatorios: usuario, nombre, email y contraseña
            adminUser.setUsername(testUsername);
            adminUser.setName("Administrador Test"); // Agregado campo Nombre
            adminUser.setEmail(testEmail);
            adminUser.setEncodedPassword(passwordEncoder.encode(testPassword));

            // Roles
            adminUser.setRoles(Set.of("ADMIN"));

            userRepository.save(adminUser);
        }

        // 2. Realizar Login para obtener la Cookie
        // Usamos el DTO LoginRequest definido abajo que usa 'username' y 'password'
        Response loginResponse = given()
                .contentType(ContentType.JSON)
                .body(new LoginRequest(testUsername, testPassword))
                .when()
                .post(BASE_URL + "/auth/login");

        // Verificamos que el login fue exitoso (200 OK)
        loginResponse.then().statusCode(200);

        // 3. Extraer el valor de la cookie de la respuesta
        String tokenCookieValue = loginResponse.getCookie(JWT_COOKIE_NAME);

        if (tokenCookieValue == null) {
            throw new RuntimeException("El login no devolvió la cookie llamada: " + JWT_COOKIE_NAME +
                    ". Verifica el nombre en tu controlador.");
        }

        // 4. Configurar RestAssured globalmente para usar esa Cookie
        RestAssured.requestSpecification = new RequestSpecBuilder()
                .setBasePath(BASE_URL + "/products")
                .setContentType(CONTENT_TYPE)
                .addCookie(JWT_COOKIE_NAME, tokenCookieValue) // Inyección de la cookie
                .build();
    }

    @Test
    public void createProductTest() {
        Response postResponse = createProduct();
        postResponse.then().statusCode(201);

        Response getResponse = getProduct(postResponse.jsonPath().getString("id"));
        getResponse.then()
                .statusCode(200)
                .body("name", equalTo(name))
                .body("description", equalTo(description))
                .body("currentPrice", equalTo((float) currentPrice));
    }

    @Test
    public void getInexistentProductTest() {
        String wrongId = "-2";
        Response getResponse = getProduct(wrongId);
        getResponse.then().statusCode(404);
    }

    @Test
    public void updateProductTest() {
        Response postResponse = createProduct();

        description = "Hasta 30 horas de reproducción de vídeo";
        Response putResponse = updateProduct(postResponse.jsonPath().getString("id"));

        putResponse.then().statusCode(202).body("description", equalTo(description));

        Response getResponse = getProduct(postResponse.jsonPath().getString("id"));
        getResponse.then()
                .statusCode(200)
                .body("description", equalTo(description));
    }

    @Test
    public void deleteProductTest() {
        Response postResponse = createProduct();
        String productId = postResponse.jsonPath().getString("id");

        Response deleteResponse = deleteProduct(productId);
        deleteResponse.then().statusCode(200);

        Response getResponse = getProduct(productId);
        getResponse.then().statusCode(404);
    }

    // --- Métodos Auxiliares ---

    private Response createProduct() {
        ProductRequest product = new ProductRequest(name, description, currentPrice);
        return given()
                .body(product)
                .when()
                .post();
    }

    private Response updateProduct(String productId) {
        ProductRequest product = new ProductRequest(name, description, currentPrice);
        return given()
                .pathParam("id", productId)
                .body(product)
                .when()
                .put("/{id}");
    }

    private Response getProduct(String productId) {
        return given()
                .pathParam("id", productId)
                .when()
                .get("/{id}");
    }

    private Response deleteProduct(String productId) {
        return given()
                .pathParam("id", productId)
                .when()
                .delete("/{id}");
    }

    // DTOs internos para mapear JSON
    private static class ProductRequest {
        public String name;
        public String description;
        public double currentPrice;

        public ProductRequest(String name, String description, double currentPrice) {
            this.name = name;
            this.description = description;
            this.currentPrice = currentPrice;
        }
    }

    private static class LoginRequest {
        public String username;
        public String password;

        public LoginRequest(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }
}