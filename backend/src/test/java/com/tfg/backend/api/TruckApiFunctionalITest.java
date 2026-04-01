package com.tfg.backend.api;

import com.tfg.backend.BackendApplication;
import com.tfg.backend.dto.AddressDTO;
import com.tfg.backend.dto.TruckDTO;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = BackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
public class TruckApiFunctionalITest {

    @LocalServerPort
    int port;

    @Autowired private UserRepository userRepository;
    @Autowired private ShopRepository shopRepository;
    @Autowired private TruckRepository truckRepository;

    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private EntityManager entityManager;

    private static final String JWT_COOKIE_NAME = "AuthToken";
    private static final String BASE_URL_AUTH = "/api/v1/auth";
    private static final String BASE_URL_TRUCKS = "/api/v1/trucks";

    private User testAdmin;
    private User testManager;
    private User testDriver;
    private User testUser;

    private Shop testShop;
    private Truck testTruck;
    private Truck unassignedTruck;

    // Cached cookies to avoid repeated login requests
    private String adminCookie;
    private String managerCookie;
    private String driverCookie;
    private String userCookie;

    @BeforeEach
    public void setUp() {
        RestAssured.reset();
        RestAssured.baseURI = "https://localhost:" + port;
        RestAssured.useRelaxedHTTPSValidation();

        // 1. Security bypass cleaning
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "pass",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );

        cleanDatabase();
        SecurityContextHolder.clearContext();

        // 2. Data creation (avoids transient object exceptions)
        transactionTemplate.executeWithoutResult(status -> {
            // Shop creation
            Address shopAddress = new Address("ShopHQ", "Main Street", "1", "1A", "28000", "Madrid", "Spain");
            testShop = new Shop("API Test Shop", shopAddress, 5000.0);
            testShop.setReferenceCode("SHP-001");
            testShop = shopRepository.save(testShop);

            // Users creation
            testAdmin = new User("Admin", "admin_trk", "admin@trk.com", passwordEncoder.encode("pass"), "ADMIN");
            testAdmin = userRepository.save(testAdmin);

            testManager = new User("Manager", "manager_trk", "manager@trk.com", passwordEncoder.encode("pass"), "MANAGER");
            testManager = userRepository.save(testManager);

            testDriver = new User("Driver", "driver_trk", "driver@trk.com", passwordEncoder.encode("pass"), "DRIVER");
            testDriver = userRepository.save(testDriver);

            testUser = new User("User", "user_trk", "user@trk.com", passwordEncoder.encode("pass"), "USER");
            testUser = userRepository.save(testUser);

            // Shop assignments
            testShop.setAssignedManager(testManager);
            testShop = shopRepository.save(testShop);

            // Trucks creation
            Address truckAddress1 = new Address("Garage 1", "Industrial St", "10", "B", "28001", "Madrid", "Spain");
            testTruck = new Truck("1111-ABC", truckAddress1, 50);
            testTruck.setAssignedShop(testShop);
            testTruck.setAssignedDriver(testDriver);
            testTruck = truckRepository.save(testTruck);

            // Unassigned truck for testing availability endpoint
            Address truckAddress2 = new Address("Garage 2", "Secondary St", "5", "A", "28002", "Madrid", "Spain");
            unassignedTruck = new Truck("2222-XYZ", truckAddress2, 30);
            unassignedTruck = truckRepository.save(unassignedTruck);

            // Safely flush within the active transaction
            entityManager.flush();
        });

        // 3. Cache login cookies
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
            // 1. Delete standalone dependent entities directly
            entityManager.createQuery("DELETE FROM OrderItem").executeUpdate();
            entityManager.createQuery("DELETE FROM Order").executeUpdate();

            // 2. Unlink relationships in the database directly via JPQL
            entityManager.createQuery("UPDATE Truck t SET t.assignedDriver = null, t.assignedShop = null").executeUpdate();
            entityManager.createQuery("UPDATE Shop s SET s.assignedManager = null").executeUpdate();

            // 3. Unlink ManyToMany relationships
            userRepository.findAll().forEach(u -> {
                u.setSelectedShop(null);
                u.getFavouriteProducts().clear();
                userRepository.save(u);
            });
            userRepository.flush();

            // 4. CRITICAL: Clear the Persistence Context memory (prevents "TransientObjectException")
            entityManager.flush();
            entityManager.clear();

            // 5. Delete entities via Repositories to handle CascadeType.ALL correctly (e.g., trucks_history)
            truckRepository.deleteAll();
            shopRepository.deleteAll();
            userRepository.deleteAll();

            // 6. Delete leftover addresses safely
            entityManager.createQuery("DELETE FROM Address").executeUpdate();

            // Final safety flush and clear
            entityManager.flush();
            entityManager.clear();
        });
    }


    // ==========================================
    // READ ENDPOINTS TESTS
    // ==========================================

    @Test
    public void getAllTrucksPage_AsAdmin_ReturnsPagedTrucks() {
        given().spec(authAsAdmin())
                .when().get("/")
                .then().statusCode(200)
                .body("items", notNullValue())
                .body("items.size()", greaterThanOrEqualTo(2)); // Both testTruck and unassignedTruck
    }

    @Test
    public void getTruckById_AsManager_ReturnsTruck() {
        given().spec(authAsManager())
                .pathParam("id", testTruck.getId())
                .when().get("/{id}")
                .then().statusCode(200)
                .body("plateNumber", equalTo("1111-ABC"))
                .body("address", notNullValue());
    }

    @Test
    public void getAssignedTruckByDriverId_AsDriver_ReturnsTruck() {
        given().spec(authAsDriver())
                .pathParam("driverId", testDriver.getId())
                .when().get("/user/{driverId}")
                .then().statusCode(200)
                .body("plateNumber", equalTo("1111-ABC"));
    }

    @Test
    public void getAllShopTrucks_AsManager_ReturnsList() {
        given().spec(authAsManager())
                .pathParam("shopId", testShop.getId())
                .when().get("/shop/{shopId}/list")
                .then().statusCode(200)
                .body("size()", greaterThan(0))
                .body("[0].plateNumber", equalTo("1111-ABC"));
    }

    @Test
    public void getTrucksByShopId_AsUser_ReturnsPagedTrucks() {
        given().spec(authAsUser())
                .pathParam("shopId", testShop.getId())
                .when().get("/shop/{shopId}")
                .then().statusCode(200)
                .body("items", notNullValue())
                .body("items[0].plateNumber", equalTo("1111-ABC"));
    }

    @Test
    public void getAllUnassignedTrucks_AsAdmin_ReturnsAvailableList() {
        given().spec(authAsAdmin())
                .when().get("/available/")
                .then().statusCode(200)
                .body("size()", greaterThan(0))
                .body("[0].plateNumber", equalTo("2222-XYZ")); // Verifies the unassigned truck is fetched
    }

    // ==========================================
    // WRITE ENDPOINTS TESTS (CRUD & Assignments)
    // ==========================================

    @Test
    public void createTruck_AsAdmin_CreatesTruck() {
        TruckDTO newTruck = new TruckDTO();
        newTruck.setPlateNumber("9999-NEW");
        newTruck.setMaxOrderCapacity(100);
        newTruck.setAddress(new AddressDTO(new Address("Depot", "Central St", "10", "A", "28005", "Madrid", "Spain")));

        given().spec(authAsAdmin())
                .body(newTruck)
                .when().post()
                .then().statusCode(201)
                .header("Location", notNullValue())
                .body("plateNumber", equalTo("9999-NEW"))
                .body("maxOrderCapacity", equalTo(100));
    }

    @Test
    public void updateTruck_AsAdmin_UpdatesTruckData() {
        // 1. Fetch the existing truck to avoid sending nulls for required fields (like referenceCode or shopId)
        TruckDTO updateData = given().spec(authAsAdmin())
                .pathParam("id", testTruck.getId())
                .when().get("/{id}")
                .then().statusCode(200)
                .extract().as(TruckDTO.class);

        // 2. Modify only the fields we want to update
        updateData.setPlateNumber("1111-MOD");
        updateData.setMaxOrderCapacity(60);
        updateData.setAddress(new AddressDTO(new Address("Depot Mod", "Mod St", "2", "B", "28005", "Madrid", "Spain")));

        // 3. Send the complete object back
        given().spec(authAsAdmin())
                .pathParam("id", testTruck.getId())
                .body(updateData)
                .when().put("/{id}")
                .then().statusCode(202) // Controller uses ResponseEntity.accepted()
                .body("plateNumber", equalTo("1111-MOD"))
                .body("maxOrderCapacity", equalTo(60));
    }

    @Test
    public void setAssignedDriver_AsAdmin_UpdatesAssignment() {
        // First unassign to clear state
        given().spec(authAsAdmin())
                .pathParam("truckId", testTruck.getId())
                .pathParam("driverId", testDriver.getId())
                .queryParam("state", false)
                .when().put("/{truckId}/assign/driver/{driverId}")
                .then().statusCode(200);

        // Then reassign
        given().spec(authAsAdmin())
                .pathParam("truckId", testTruck.getId())
                .pathParam("driverId", testDriver.getId())
                .queryParam("state", true)
                .when().put("/{truckId}/assign/driver/{driverId}")
                .then().statusCode(200)
                .body("assignedDriver.id", equalTo(testDriver.getId().intValue()));
    }

    @Test
    public void commentAndOrUpdateTruckStatus_AsAdmin_UpdatesStatus() {
        String responseBody = given().spec(authAsAdmin())
                .pathParam("id", testTruck.getId())
                .queryParam("truckStatus", "MAINTENANCE")
                .queryParam("comment", "Going to the mechanic")
                .when().put("/status/{id}")
                .then().statusCode(200)
                .extract().asString();

        // Ensure the comment was successfully appended to the history and returned
        assertTrue(
                responseBody.contains("Going to the mechanic"),
                "The truck history should contain the new status comment"
        );
    }

    @Test
    public void deleteTruck_AsAdmin_DeletesTruck() {
        given().spec(authAsAdmin())
                .pathParam("id", testTruck.getId())
                .when().delete("/{id}")
                .then().statusCode(200);

        // Verify it was deleted (Should return 404 based on controller logic)
        given().spec(authAsAdmin())
                .pathParam("id", testTruck.getId())
                .when().get("/{id}")
                .then().statusCode(404);
    }


    // ==========================================
    // AUTHENTICATION HELPERS
    // ==========================================

    private String loginAndGetCookie(String username, String password) {
        return given().contentType(ContentType.JSON).body(new LoginRequest(username, password))
                .when().post(BASE_URL_AUTH + "/login").getCookie(JWT_COOKIE_NAME);
    }

    private RequestSpecification authAsAdmin() { return new RequestSpecBuilder().setBasePath(BASE_URL_TRUCKS).setContentType(ContentType.JSON).addCookie(JWT_COOKIE_NAME, adminCookie).build(); }
    private RequestSpecification authAsManager() { return new RequestSpecBuilder().setBasePath(BASE_URL_TRUCKS).setContentType(ContentType.JSON).addCookie(JWT_COOKIE_NAME, managerCookie).build(); }
    private RequestSpecification authAsDriver() { return new RequestSpecBuilder().setBasePath(BASE_URL_TRUCKS).setContentType(ContentType.JSON).addCookie(JWT_COOKIE_NAME, driverCookie).build(); }
    private RequestSpecification authAsUser() { return new RequestSpecBuilder().setBasePath(BASE_URL_TRUCKS).setContentType(ContentType.JSON).addCookie(JWT_COOKIE_NAME, userCookie).build(); }
}