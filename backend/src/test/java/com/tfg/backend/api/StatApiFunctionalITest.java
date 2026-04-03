package com.tfg.backend.api;

import com.tfg.backend.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class StatApiFunctionalITest extends BaseApiFunctionalITest {

    private static final String BASE_URL_STATS = "/api/v1/stats";

    private String adminCookie;
    private String managerCookie;
    private String driverCookie;

    @BeforeEach
    public void setUp() {
        transactionTemplate.executeWithoutResult(status -> {
            User testAdmin = new User("Admin", "admin_stat", "admin@stat.com", passwordEncoder.encode("pass"), "ADMIN");
            userRepository.saveAndFlush(testAdmin);

            User testManager = new User("Manager", "manager_stat", "manager@stat.com", passwordEncoder.encode("pass"), "MANAGER");
            userRepository.saveAndFlush(testManager);

            User testDriver = new User("Driver", "driver_stat", "driver@stat.com", passwordEncoder.encode("pass"), "DRIVER");
            userRepository.saveAndFlush(testDriver);
        });

        adminCookie = loginAndGetCookie("admin_stat", "pass");
        managerCookie = loginAndGetCookie("manager_stat", "pass");
        driverCookie = loginAndGetCookie("driver_stat", "pass");
    }

    @Test
    public void getOrdersStats_AsAdmin_ReturnsList() {
        given().spec(getSpec(BASE_URL_STATS, adminCookie))
                .when().get("/orders")
                .then().statusCode(200).body("[0].label", notNullValue());
    }

    @Test
    public void getOrdersStats_AsManager_ReturnsList() {
        given().spec(getSpec(BASE_URL_STATS, managerCookie))
                .when().get("/orders")
                .then().statusCode(200).body("[0].label", notNullValue());
    }

    @Test
    public void getOrdersStats_AsDriver_ReturnsList() {
        given().spec(getSpec(BASE_URL_STATS, driverCookie))
                .when().get("/orders")
                .then().statusCode(200).body("[0].label", notNullValue());
    }

    @Test
    public void getShopsStats_AsAdmin_ReturnsList() {
        given().spec(getSpec(BASE_URL_STATS, adminCookie))
                .when().get("/shops")
                .then().statusCode(200).body("[0].label", notNullValue());
    }

    @Test
    public void getShopsStats_AsManager_ReturnsList() {
        given().spec(getSpec(BASE_URL_STATS, managerCookie))
                .when().get("/shops")
                .then().statusCode(200).body("[0].label", notNullValue());
    }

    @Test
    public void getTrucksStats_AsAdmin_ReturnsList() {
        given().spec(getSpec(BASE_URL_STATS, adminCookie))
                .when().get("/trucks")
                .then().statusCode(200).body("[0].label", notNullValue());
    }

    @Test
    public void getTrucksStats_AsManager_ReturnsList() {
        given().spec(getSpec(BASE_URL_STATS, managerCookie))
                .when().get("/trucks")
                .then().statusCode(200).body("[0].label", notNullValue());
    }
}