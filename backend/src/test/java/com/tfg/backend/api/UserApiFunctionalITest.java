package com.tfg.backend.api;

import com.tfg.backend.BackendApplication;
import com.tfg.backend.dto.AddressDTO;
import com.tfg.backend.dto.PaymentCardDTO;
import com.tfg.backend.dto.UserDTO;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(
        classes = BackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
public class UserApiFunctionalITest {

    @LocalServerPort
    int port;

    @Autowired private UserRepository userRepository;
    @Autowired private ShopRepository shopRepository;

    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private EntityManager entityManager;

    private static final String JWT_COOKIE_NAME = "AuthToken";
    private static final String BASE_URL_AUTH = "/api/v1/auth";
    private static final String BASE_URL_USERS = "/api/v1/users";

    private User testAdmin;
    private User testUser;
    private User testDriver;
    private Shop testShop;

    // Track sub-resource IDs for PUT/DELETE testing
    private Long testAddressId;
    private Long testCardId;

    // Cached cookies to avoid repeated login requests
    private String adminCookie;
    private String userCookie;

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

        // 2. DATA CREATION (Wrapped in a transaction to safely handle complex relationships)
        transactionTemplate.executeWithoutResult(status -> {
            // --- SHOP CREATION ---
            Address shopAddress = new Address("ShopHQ", "Main Street", "1", "1A", "28000", "Madrid", "Spain");
            testShop = new Shop("API Test Shop", shopAddress, 5000.0);
            testShop.setReferenceCode("SHP-001");
            testShop = shopRepository.save(testShop);

            // --- ADMIN CREATION ---
            testAdmin = new User("Admin", "admin_usr", "admin@usr.com", passwordEncoder.encode("pass"), "ADMIN");
            testAdmin = userRepository.save(testAdmin);

            // --- DRIVER CREATION (Used for available drivers endpoint) ---
            testDriver = new User("Driver", "driver_usr", "driver@usr.com", passwordEncoder.encode("pass"), "DRIVER");
            testDriver = userRepository.save(testDriver);

            // --- STANDARD USER CREATION (With Address and Card) ---
            testUser = new User("User", "user_usr", "user@usr.com", passwordEncoder.encode("pass"), "USER");

            Address userAddress = new Address("Home", "User St", "5", "B", "28005", "Madrid", "Spain");
            testUser.getAddresses().add(userAddress);

            PaymentCard userCard = new PaymentCard("Personal Card", "User Name", "1234567812345678", "123", YearMonth.of(2028, 12));
            testUser.getCards().add(userCard);

            testUser = userRepository.save(testUser);

            // Safely flush within the active transaction
            entityManager.flush();

            // Store the generated IDs from the database to test the PUT/DELETE endpoints
            testAddressId = testUser.getAddresses().getFirst().getId();
            testCardId = testUser.getCards().getFirst().getId();
        });

        // 3. CACHE LOGIN COOKIES (Executed only once per test setup)
        adminCookie = loginAndGetCookie(testAdmin.getUsername(), "pass");
        userCookie = loginAndGetCookie(testUser.getUsername(), "pass");
    }

    @AfterEach
    public void tearDown() {
        cleanDatabase();
    }

    /**
     * Safely breaks relationships and clears memory before wiping out data.
     */
    private void cleanDatabase() {
        transactionTemplate.executeWithoutResult(status -> {
            entityManager.createQuery("DELETE FROM OrderItem").executeUpdate();
            entityManager.createQuery("DELETE FROM Order").executeUpdate();
            entityManager.createQuery("DELETE FROM Review").executeUpdate();

            // Unlink relationships to avoid constraints
            entityManager.createQuery("UPDATE Truck t SET t.assignedDriver = null, t.assignedShop = null").executeUpdate();
            entityManager.createQuery("UPDATE Shop s SET s.assignedManager = null").executeUpdate();

            userRepository.findAll().forEach(u -> {
                u.setSelectedShop(null);
                u.getFavouriteProducts().clear();
                userRepository.save(u);
            });
            userRepository.flush();

            // Clear Persistence Context to avoid TransientObjectExceptions
            entityManager.flush();
            entityManager.clear();

            // Delete entities via Repositories to trigger Cascade rules (like deleting Addresses and Cards)
            shopRepository.deleteAll();
            userRepository.deleteAll();

            // Fallback for orphaned addresses
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

    private RequestSpecification authAsAdmin() { return new RequestSpecBuilder().setBasePath(BASE_URL_USERS).setContentType(ContentType.JSON).addCookie(JWT_COOKIE_NAME, adminCookie).build(); }
    private RequestSpecification authAsUser() { return new RequestSpecBuilder().setBasePath(BASE_URL_USERS).setContentType(ContentType.JSON).addCookie(JWT_COOKIE_NAME, userCookie).build(); }

    // ==========================================
    // READ ENDPOINTS TESTS
    // ==========================================

    @Test
    public void getSessionInfo_AsUser_ReturnsUserLoginDTO() {
        given().spec(authAsUser())
                .when().get("/session")
                .then().statusCode(200)
                .body("username", equalTo("user_usr"))
                .body("roles", hasItem("USER")); //
    }

    @Test
    public void getLoggedUser_AsUser_ReturnsUserDTO() {
        given().spec(authAsUser())
                .when().get("/me")
                .then().statusCode(200)
                .body("username", equalTo("user_usr"))
                .body("addresses.size()", greaterThan(0)) // Verifying relations were loaded
                .body("cards.size()", greaterThan(0));
    }

    @Test
    public void getAvailableDrivers_AsAdmin_ReturnsDriversList() {
        given().spec(authAsAdmin())
                .when().get("/drivers/available/")
                .then().statusCode(200)
                .body("size()", greaterThan(0))
                .body("[0].username", equalTo("driver_usr"));
    }

    @Test
    public void getAllUsersByRole_AsAdmin_ReturnsList() {
        given().spec(authAsAdmin())
                .queryParam("role", "ADMIN")
                .when().get("/role/")
                .then().statusCode(200)
                .body("size()", greaterThan(0))
                .body("[0].username", equalTo("admin_usr"));
    }

    @Test
    public void getAllUsers_AsAdmin_ReturnsPagedUsers() {
        given().spec(authAsAdmin())
                .when().get("/")
                .then().statusCode(200)
                .body("items", notNullValue())
                .body("items.size()", greaterThanOrEqualTo(2));
    }

    @Test
    public void checkUsernameAndEmail_ReturnsBoolean() {
        given().spec(authAsUser())
                .queryParam("username", "user_usr")
                .when().get("/username")
                .then().statusCode(200)
                .body(equalTo("true"));

        given().spec(authAsUser())
                .queryParam("email", "unknown@email.com")
                .when().get("/email")
                .then().statusCode(200)
                .body(equalTo("false"));
    }

    // ==========================================
    // UPDATE PROFILE & SHOP TESTS
    // ==========================================

    @Test
    public void setSelectedShop_AsUser_UpdatesSelection() {
        Map<String, Long> requestBody = new HashMap<>();
        requestBody.put("shopId", testShop.getId());

        given().spec(authAsUser())
                .body(requestBody)
                .when().put("/shop")
                .then().statusCode(200)
                .body(equalTo("true"));
    }

    @Test
    public void updateLoggedUserData_AsUser_UpdatesInfo() {
        UserDTO updateData = new UserDTO();
        updateData.setName("Updated Name");
        updateData.setUsername("user_usr"); // Keep the same to avoid 403 Forbidden
        updateData.setEmail("updated@usr.com");
        updateData.setPhone("123456789");

        given().spec(authAsUser())
                .body(updateData)
                .when().put("/data")
                .then().statusCode(200)
                .body("name", equalTo("Updated Name"))
                .body("email", equalTo("updated@usr.com"));
    }

    // ==========================================
    // ADDRESSES TESTS
    // ==========================================

    @Test
    public void createAddress_AsUser_CreatesAddress() {
        AddressDTO newAddress = new AddressDTO();
        newAddress.setAlias("Work");
        newAddress.setStreet("Work St");
        newAddress.setNumber("10");
        newAddress.setCity("Madrid");
        newAddress.setCountry("Spain");

        given().spec(authAsUser())
                .body(newAddress)
                .when().post("/addresses")
                .then().statusCode(201)
                .body("addresses.size()", equalTo(2)) // 1 existing + 1 new
                .body("addresses.alias", hasItem("Work"));
    }

    @Test
    public void editAddress_AsUser_UpdatesAddress() {
        AddressDTO editAddress = new AddressDTO();
        editAddress.setId(testAddressId);
        editAddress.setAlias("Home Updated");
        editAddress.setStreet("User St");
        editAddress.setNumber("5");

        given().spec(authAsUser())
                .body(editAddress)
                .when().put("/addresses")
                .then().statusCode(200)
                .body("addresses.alias", hasItem("Home Updated"));
    }

    @Test
    public void deleteAddress_AsUser_RemovesAddress() {
        given().spec(authAsUser())
                .pathParam("id", testAddressId)
                .when().delete("/addresses/{id}")
                .then().statusCode(200)
                .body("addresses.size()", equalTo(0)); // Should be empty now
    }

    // ==========================================
    // CARDS TESTS
    // ==========================================

    @Test
    public void createPaymentCard_AsUser_CreatesCard() {
        PaymentCardDTO newCard = new PaymentCardDTO();
        newCard.setAlias("Business Card");
        newCard.setCardOwnerName("User Name");
        newCard.setNumber("1111222233334444");
        newCard.setCvv("999");
        newCard.setDueDate("05/29"); // Required format MM/yy

        given().spec(authAsUser())
                .body(newCard)
                .when().post("/cards")
                .then().statusCode(201)
                .body("cards.size()", equalTo(2))
                .body("cards.alias", hasItem("Business Card"));
    }

    @Test
    public void editPaymentCard_AsUser_UpdatesCard() {
        PaymentCardDTO editCard = new PaymentCardDTO();
        editCard.setId(testCardId);
        editCard.setAlias("Personal Updated");
        editCard.setCardOwnerName("New Name");
        editCard.setDueDate("10/30");

        given().spec(authAsUser())
                .body(editCard)
                .when().put("/cards")
                .then().statusCode(200)
                .body("cards.alias", hasItem("Personal Updated"));
    }

    @Test
    public void deletePaymentCard_AsUser_RemovesCard() {
        given().spec(authAsUser())
                .pathParam("id", testCardId)
                .when().delete("/cards/{id}")
                .then().statusCode(200)
                .body("cards.size()", equalTo(0));
    }

    // ==========================================
    // ADMIN ACTIONS (BAN, ANON, DELETE)
    // ==========================================

    @Test
    public void toggleUserBanById_AsAdmin_ChangesBanState() {
        given().spec(authAsAdmin())
                .pathParam("id", testUser.getId())
                .body(true) // banState
                .when().put("/ban/{id}")
                .then().statusCode(200)
                .body("banned", equalTo(true));
    }

    @Test
    public void anonymizeUserById_AsAdmin_AnonymizesData() {
        given().spec(authAsAdmin())
                .pathParam("id", testUser.getId())
                .when().put("/anon/{id}")
                .then().statusCode(200)
                .body("deleted", equalTo(true))
                .body("email", containsString("deleteduser_")) //
                .body("addresses.size()", equalTo(0)) // Ensures sensitive data is wiped
                .body("cards.size()", equalTo(0));
    }

    @Test
    public void deleteUserById_AsAdmin_DeletesUser() {
        given().spec(authAsAdmin())
                .pathParam("id", testUser.getId())
                .when().delete("/{id}")
                .then().statusCode(200)
                .body(equalTo("true"));

        // Verify the user is gone by trying to access it
        given().spec(authAsAdmin())
                .queryParam("username", "user_usr")
                .when().get("/username")
                .then().statusCode(200)
                .body(equalTo("false"));
    }

    // ==========================================
    // MULTIPART IMAGE TESTS
    // ==========================================

    @Test
    public void uploadUserImage_AsUser_UploadsMultipart() {
        given().spec(authAsUser())
                .contentType("multipart/form-data")
                .pathParam("id", testUser.getId())
                .multiPart("image", "avatar.jpg", "fake_image_content".getBytes(), "image/jpeg")
                .when().put("/image/{id}")
                .then().statusCode(200)
                .body("imageInfo", notNullValue());
    }
}