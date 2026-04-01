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
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

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

    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private static final String JWT_COOKIE_NAME = "AuthToken";
    private static final String CONTENT_TYPE = "application/json";
    private static final String BASE_URL_AUTH = "/api/v1/auth";
    private static final String BASE_URL_CATEGORIES = "/api/v1/categories";

    private User testAdmin;
    private Category otrosCategory;

    @BeforeEach
    public void setUp() {
        RestAssured.reset();
        RestAssured.baseURI = "https://localhost:" + port;
        RestAssured.useRelaxedHTTPSValidation();

        // Limpiar categorías por si quedó basura de tests anteriores
        categoryRepository.deleteAll();

        // 2. CRÍTICO: Crear siempre la categoría "Otros", el servicio la necesita
        otrosCategory = new Category("Otros", "icon", "banner", "Default category", "Default");
        categoryRepository.save(otrosCategory);

        String testUsername = "admin_category_test";
        if (!userRepository.existsByUsername(testUsername)) {
            testAdmin = new User("Category Admin", testUsername, "admin_category@test.com", passwordEncoder.encode("password123"), "ADMIN");
            userRepository.save(testAdmin);
        }

        Response loginResponse = given()
                .contentType(ContentType.JSON)
                .body(new LoginRequest(testUsername, "password123"))
                .when()
                .post(BASE_URL_AUTH + "/login");

        String tokenCookieValue = loginResponse.getCookie(JWT_COOKIE_NAME);

        RestAssured.requestSpecification = new RequestSpecBuilder()
                .setBasePath(BASE_URL_CATEGORIES)
                .setContentType(CONTENT_TYPE)
                .addCookie(JWT_COOKIE_NAME, tokenCookieValue)
                .build();
    }

    @AfterEach
    public void tearDown() {
        // Limpiamos los datos reales insertados por la API
        categoryRepository.deleteAll();
        if (testAdmin != null) userRepository.delete(testAdmin);
    }

    // --- TESTS ORIGINALES MEJORADOS ---

    @Test
    public void createCategoryTest() {
        CategoryDTO dto = new CategoryDTO();
        dto.setName("Electrodomésticos ITest");
        dto.setShortDescription("Testing category");

        given().body(dto).when().post()
                .then().statusCode(201)
                .body("name", equalTo("Electrodomésticos ITest"));
    }

    @Test
    public void updateOtrosCategoryForbiddenTest() {
        // ELIMINADO EL IF. El test falla si "Otros" no existe, que es lo correcto.
        CategoryDTO dto = new CategoryDTO();
        dto.setId(otrosCategory.getId());
        dto.setName("Modified Otros");

        given()
                .pathParam("id", otrosCategory.getId())
                .body(dto)
                .when()
                .put("/{id}")
                .then()
                .statusCode(403);
    }

    @Test
    public void deleteOtrosCategoryForbiddenTest() {
        given()
                .pathParam("id", otrosCategory.getId())
                .when()
                .delete("/{id}")
                .then()
                .statusCode(403);
    }

    // --- NUEVOS TESTS DE LÓGICA DE NEGOCIO ---

    @Test
    public void createSubCategoryTest() {
        // 1. Crear padre
        CategoryDTO parentDto = new CategoryDTO();
        parentDto.setName("Electrónica");
        Long parentId = given().body(parentDto).when().post().jsonPath().getLong("id");

        // 2. Crear hijo asignando el parentId
        CategoryDTO childDto = new CategoryDTO();
        childDto.setName("Móviles");
        childDto.setParentId(parentId);

        given().body(childDto).when().post()
                .then().statusCode(201)
                .body("parentId", equalTo(parentId.intValue()));
    }

    @Test
    public void circularReferenceForbiddenTest() {
        // 1. Crear Abuelo
        CategoryDTO abueloDto = new CategoryDTO();
        abueloDto.setName("Abuelo");
        Long abueloId = given().body(abueloDto).when().post().jsonPath().getLong("id");

        // 2. Crear Padre
        CategoryDTO padreDto = new CategoryDTO();
        padreDto.setName("Padre");
        padreDto.setParentId(abueloId);
        Long padreId = given().body(padreDto).when().post().jsonPath().getLong("id");

        // 3. Intento de Circularidad: Actualizar Abuelo para que su padre sea su propio hijo
        abueloDto.setId(abueloId);
        abueloDto.setParentId(padreId); // Intentamos meter a Abuelo dentro de Padre

        given()
                .pathParam("id", abueloId)
                .body(abueloDto)
                .when()
                .put("/{id}")
                .then()
                .statusCode(403); // El validador circular debe interceptarlo
    }
}