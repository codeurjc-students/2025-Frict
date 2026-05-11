package com.tfg.backend.api;

import com.tfg.backend.dto.EntityType;
import com.tfg.backend.model.Registry;
import com.tfg.backend.model.RegistryType;
import com.tfg.backend.model.User;
import com.tfg.backend.service.RegistryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class RegistryApiFunctionalITest extends BaseApiFunctionalITest {

    private static final String BASE_URL_REGISTRY = "/api/v1/registry";

    @Autowired private RegistryService registryService;
    @Autowired private MongoTemplate mongoTemplate;

    private String adminCookie;
    private String managerCookie;
    private String driverCookie;
    private String userCookie;

    private String startDateStr;
    private String endDateStr;

    @BeforeEach
    public void setUp() {
        // 0. Purge any registries left over by previous test classes whose async
        //    @TransactionalEventListener writes may have landed after their teardown.
        mongoTemplate.remove(new Query(), "registries");

        // 1. Setup MySQL users with different roles
        transactionTemplate.executeWithoutResult(status -> {
            userRepository.saveAndFlush(new User("Admin", "admin_reg", "admin@reg.com", passwordEncoder.encode("pass"), "ADMIN"));
            userRepository.saveAndFlush(new User("Manager", "manager_reg", "manager@reg.com", passwordEncoder.encode("pass"), "MANAGER"));
            userRepository.saveAndFlush(new User("Driver", "driver_reg", "driver@reg.com", passwordEncoder.encode("pass"), "DRIVER"));
            userRepository.saveAndFlush(new User("User", "user_reg", "user@reg.com", passwordEncoder.encode("pass"), "USER"));
        });

        // 2. Setup a wide date window and seed MongoDB with registry data
        // Spring's DateFormatter for java.util.Date requires exactly 3 fractional digits (SSS);
        // Instant.toString() may produce 6-9 digits, causing a 400 MethodArgumentTypeMismatchException.
        DateTimeFormatter isoMillis = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);
        startDateStr = isoMillis.format(Instant.now().minusSeconds(120));
        endDateStr = isoMillis.format(Instant.now().plusSeconds(120));

        Registry productView = new Registry(
                EntityType.PRODUCT, RegistryType.PRODUCT_VIEWS, 5.0,
                "shop-reg-1", "Shop 1", "user-reg-1", "User 1",
                "prod-reg-1", "Product 1", null, null);
        productView.getMetrics().setTotal(50.0);
        registryService.save(productView);

        // 3. Obtain JWT cookies for each role
        adminCookie = loginAndGetCookie("admin_reg", "pass");
        managerCookie = loginAndGetCookie("manager_reg", "pass");
        driverCookie = loginAndGetCookie("driver_reg", "pass");
        userCookie = loginAndGetCookie("user_reg", "pass");
    }

    @AfterEach
    public void tearDownMongoDB() {
        mongoTemplate.remove(new Query(), "registries");
    }

    // --- PUBLIC VIEWS ---

    @Test
    public void getPublicViews_NoAuth_Returns200WithData() {
        given().spec(getSpec(BASE_URL_REGISTRY, null))
                .queryParam("startDate", startDateStr)
                .queryParam("endDate", endDateStr)
                .queryParam("viewType", "TABLE")
                .queryParam("entityType", "PRODUCT")
                .queryParam("dataType", "PRODUCT_VIEWS")
                .queryParam("productIds", "prod-reg-1")
                .when().get("/public/views")
                .then().statusCode(200)
                .body("totalItems", greaterThanOrEqualTo(1));
    }

    @Test
    public void getPublicViews_WrongEntityType_ReturnsForbidden() {
        // Public endpoint is restricted to PRODUCT / PRODUCT_VIEWS only
        given().spec(getSpec(BASE_URL_REGISTRY, null))
                .queryParam("startDate", startDateStr)
                .queryParam("endDate", endDateStr)
                .queryParam("viewType", "TABLE")
                .queryParam("entityType", "ORDER")
                .queryParam("dataType", "ORDERS_COMPLETED")
                .queryParam("productIds", "any-id")
                .when().get("/public/views")
                .then().statusCode(403);
    }

    // --- PRIVATE STATS ---

    @Test
    public void getPrivateStats_AsAdmin_Returns200() {
        given().spec(getSpec(BASE_URL_REGISTRY, adminCookie))
                .queryParam("startDate", startDateStr)
                .queryParam("endDate", endDateStr)
                .queryParam("viewType", "TABLE")
                .when().get("/private/stats")
                .then().statusCode(200)
                .body("totalItems", greaterThanOrEqualTo(1));
    }

    @Test
    public void getPrivateStats_AsManager_Returns200() {
        given().spec(getSpec(BASE_URL_REGISTRY, managerCookie))
                .queryParam("startDate", startDateStr)
                .queryParam("endDate", endDateStr)
                .queryParam("viewType", "TABLE")
                .when().get("/private/stats")
                .then().statusCode(200);
    }

    @Test
    public void getPrivateStats_AsDriver_Returns200() {
        given().spec(getSpec(BASE_URL_REGISTRY, driverCookie))
                .queryParam("startDate", startDateStr)
                .queryParam("endDate", endDateStr)
                .queryParam("viewType", "TABLE")
                .when().get("/private/stats")
                .then().statusCode(200);
    }

    @Test
    public void getPrivateStats_AsUser_ReturnsForbidden() {
        given().spec(getSpec(BASE_URL_REGISTRY, userCookie))
                .queryParam("startDate", startDateStr)
                .queryParam("endDate", endDateStr)
                .queryParam("viewType", "TABLE")
                .when().get("/private/stats")
                .then().statusCode(403);
    }

    @Test
    public void getPrivateStats_Unauthenticated_Returns401() {
        given().spec(getSpec(BASE_URL_REGISTRY, null))
                .queryParam("startDate", startDateStr)
                .queryParam("endDate", endDateStr)
                .queryParam("viewType", "TABLE")
                .when().get("/private/stats")
                .then().statusCode(401);
    }

    @Test
    public void getPrivateStats_FilteredByEntityAndDataType_ReturnsFilteredResults() {
        given().spec(getSpec(BASE_URL_REGISTRY, adminCookie))
                .queryParam("startDate", startDateStr)
                .queryParam("endDate", endDateStr)
                .queryParam("viewType", "TABLE")
                .queryParam("entityType", "PRODUCT")
                .queryParam("dataType", "PRODUCT_VIEWS")
                .when().get("/private/stats")
                .then().statusCode(200)
                .body("totalItems", equalTo(1));
    }

    // --- PRIVATE ENTITIES ---

    @Test
    public void getAvailableEntities_AsAdmin_ReturnsEntityList() {
        given().spec(getSpec(BASE_URL_REGISTRY, adminCookie))
                .when().get("/private/entities")
                .then().statusCode(200)
                .body("size()", greaterThan(0));
    }

    @Test
    public void getAvailableEntities_AsUser_ReturnsForbidden() {
        given().spec(getSpec(BASE_URL_REGISTRY, userCookie))
                .when().get("/private/entities")
                .then().statusCode(403);
    }

    // --- PRIVATE METRICS ---

    @Test
    public void getAvailableMetrics_AsAdmin_ReturnsMetricsList() {
        given().spec(getSpec(BASE_URL_REGISTRY, adminCookie))
                .queryParam("entityType", "PRODUCT")
                .when().get("/private/metrics")
                .then().statusCode(200)
                .body("size()", greaterThan(0));
    }

    // --- PRIVATE REFERENCES ---

    @Test
    public void getReferences_AsManager_ReturnsReferenceMap() {
        given().spec(getSpec(BASE_URL_REGISTRY, managerCookie))
                .queryParam("entityType", "PRODUCT")
                .queryParam("dataType", "PRODUCT_VIEWS")
                .when().get("/private/references")
                .then().statusCode(200)
                .body("productId", notNullValue())
                .body("shopId", notNullValue());
    }

    // --- PRIVATE PDF EXPORT ---

    @Test
    public void exportPdfReport_AsAdmin_ReturnsPdfContent() {
        long now = System.currentTimeMillis();

        given().spec(getSpec(BASE_URL_REGISTRY, adminCookie))
                .body(Map.of(
                        "startDate", now - 120_000,
                        "endDate", now + 120_000,
                        "entityType", "PRODUCT",
                        "dataType", "PRODUCT_VIEWS",
                        "metricMode", "VALUE"
                ))
                .when().post("/private/export/pdf")
                .then().statusCode(200)
                .header("Content-Type", containsString("application/pdf"));
    }

    @Test
    public void exportPdfReport_AsManager_ReturnsForbidden() {
        long now = System.currentTimeMillis();

        given().spec(getSpec(BASE_URL_REGISTRY, managerCookie))
                .body(Map.of(
                        "startDate", now - 120_000,
                        "endDate", now + 120_000,
                        "entityType", "PRODUCT",
                        "dataType", "PRODUCT_VIEWS",
                        "metricMode", "VALUE"
                ))
                .when().post("/private/export/pdf")
                .then().statusCode(403);
    }
}