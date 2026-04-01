package com.tfg.backend.api;

import com.tfg.backend.BackendApplication;
import com.tfg.backend.dto.UserSignupDTO;
import com.tfg.backend.model.User;
import com.tfg.backend.repository.UserRepository;
import com.tfg.backend.security.jwt.LoginRequest;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(
        classes = BackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test") // CRÍTICO: Asegura que usa la BD de test aislada
public class AuthApiFunctionalITest {

    @LocalServerPort
    int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String BASE_URL = "/api/v1/auth";
    private static final String CONTENT_TYPE = "application/json";

    // Credenciales del usuario por defecto para las pruebas
    private static final String TEST_USER = "auth_user";
    private static final String TEST_PASS = "password123";

    @BeforeEach
    public void setUp() {
        RestAssured.reset();
        RestAssured.baseURI = "https://localhost:" + port;
        RestAssured.useRelaxedHTTPSValidation();

        // 1. Setup limpio: Creamos SIEMPRE un usuario válido antes de empezar el test
        User standardUser = new User("Auth Test User", TEST_USER, "auth@test.com", passwordEncoder.encode(TEST_PASS), "USER");
        userRepository.save(standardUser);
    }

    @AfterEach
    public void tearDown() {
        // 2. Limpieza absoluta: Vaciamos la tabla de usuarios tras cada test
        // Esto garantiza que el test de "signup" no choque nunca con usuarios anteriores
        userRepository.deleteAll();
    }

    @Test
    public void registerUserTest() {
        // Usamos un username distinto al del setUp para probar el registro correctamente
        UserSignupDTO signupDTO = new UserSignupDTO("New Signup User", "new_signup", "password123", "newsignup@test.com", "USER");

        given()
                .contentType(CONTENT_TYPE)
                .body(signupDTO)
                .when()
                .post(BASE_URL + "/signup")
                .then()
                .statusCode(201)
                .header("Location", notNullValue());
    }

    @Test
    public void loginSuccessTest() {
        given()
                .contentType(CONTENT_TYPE)
                .body(new LoginRequest(TEST_USER, TEST_PASS))
                .when()
                .post(BASE_URL + "/login")
                .then()
                .statusCode(200)
                .cookie("AuthToken", notNullValue())
                .cookie("RefreshToken", notNullValue());
    }

    @Test
    public void loginFailWrongPasswordTest() {
        given()
                .contentType(CONTENT_TYPE)
                .body(new LoginRequest(TEST_USER, "wrong_password_here"))
                .when()
                .post(BASE_URL + "/login")
                .then()
                .statusCode(401); // Unauthorized
    }

    @Test
    public void logoutTest() {
        // Obtenemos las cookies haciendo un login previo real
        Response loginResponse = performLogin();

        String authToken = loginResponse.getCookie("AuthToken");
        String refreshToken = loginResponse.getCookie("RefreshToken");

        given()
                .cookie("AuthToken", authToken)
                .cookie("RefreshToken", refreshToken)
                .when()
                .post(BASE_URL + "/logout")
                .then()
                .statusCode(200);
    }

    @Test
    public void refreshTokenTest() {
        Response loginResponse = performLogin();
        String authToken = loginResponse.getCookie("AuthToken");
        String refreshToken = loginResponse.getCookie("RefreshToken");

        given()
                .cookie("AuthToken", authToken)
                .cookie("RefreshToken", refreshToken)
                .when()
                .post(BASE_URL + "/refresh")
                .then()
                .statusCode(200)
                .cookie("AuthToken", notNullValue());
    }

    @Test
    public void recoverPasswordEndpointTest() {
        Map<String, String> payload = new HashMap<>();
        payload.put("username", TEST_USER);

        given()
                .contentType(CONTENT_TYPE)
                .body(payload)
                .when()
                .post(BASE_URL + "/recovery")
                .then()
                .statusCode(200);
    }

    // --- Método Auxiliar ---

    /**
     * Realiza una petición de login para extraer las cookies necesarias
     * en los tests que requieren autenticación previa (logout, refresh).
     */
    private Response performLogin() {
        return given()
                .contentType(CONTENT_TYPE)
                .body(new LoginRequest(AuthApiFunctionalITest.TEST_USER, AuthApiFunctionalITest.TEST_PASS))
                .when()
                .post(BASE_URL + "/login");
    }
}