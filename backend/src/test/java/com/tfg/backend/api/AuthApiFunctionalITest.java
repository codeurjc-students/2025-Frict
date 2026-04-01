package com.tfg.backend.api;

import com.tfg.backend.dto.UserSignupDTO;
import com.tfg.backend.model.User;
import com.tfg.backend.security.jwt.LoginRequest;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

public class AuthApiFunctionalITest extends BaseApiFunctionalITest {

    private static final String BASE_URL = "/api/v1/auth";
    private static final String CONTENT_TYPE = "application/json";
    private static final String TEST_USER = "auth_user";
    private static final String TEST_PASS = "password123";

    @BeforeEach
    public void setUp() {
        transactionTemplate.executeWithoutResult(status -> {
            User standardUser = new User("Auth Test User", TEST_USER, "auth@test.com", passwordEncoder.encode(TEST_PASS), "USER");
            userRepository.saveAndFlush(standardUser);
        });
    }

    @Test
    public void registerUserTest() {
        UserSignupDTO signupDTO = new UserSignupDTO("New Signup User", "new_signup", "password123", "newsignup@test.com", "USER");

        given().contentType(CONTENT_TYPE).body(signupDTO)
                .when().post(BASE_URL + "/signup")
                .then().statusCode(201).header("Location", notNullValue());
    }

    @Test
    public void loginSuccessTest() {
        given().contentType(CONTENT_TYPE).body(new LoginRequest(TEST_USER, TEST_PASS))
                .when().post(BASE_URL + "/login")
                .then().statusCode(200).cookie("AuthToken", notNullValue()).cookie("RefreshToken", notNullValue());
    }

    @Test
    public void loginFailWrongPasswordTest() {
        given().contentType(CONTENT_TYPE).body(new LoginRequest(TEST_USER, "wrong_password_here"))
                .when().post(BASE_URL + "/login")
                .then().statusCode(401);
    }

    @Test
    public void logoutTest() {
        Response loginResponse = performLogin();
        given().cookie("AuthToken", loginResponse.getCookie("AuthToken")).cookie("RefreshToken", loginResponse.getCookie("RefreshToken"))
                .when().post(BASE_URL + "/logout")
                .then().statusCode(200);
    }

    @Test
    public void refreshTokenTest() {
        Response loginResponse = performLogin();
        given().cookie("AuthToken", loginResponse.getCookie("AuthToken")).cookie("RefreshToken", loginResponse.getCookie("RefreshToken"))
                .when().post(BASE_URL + "/refresh")
                .then().statusCode(200).cookie("AuthToken", notNullValue());
    }

    @Test
    public void recoverPasswordEndpointTest() {
        Map<String, String> payload = new HashMap<>();
        payload.put("username", TEST_USER);

        given().contentType(CONTENT_TYPE).body(payload)
                .when().post(BASE_URL + "/recovery")
                .then().statusCode(200);
    }

    private Response performLogin() {
        return given().contentType(CONTENT_TYPE).body(new LoginRequest(TEST_USER, TEST_PASS))
                .when().post(BASE_URL + "/login");
    }
}