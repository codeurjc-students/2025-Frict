package com.tfg.backend.api;

import com.tfg.backend.dto.AddressDTO;
import com.tfg.backend.dto.PaymentCardDTO;
import com.tfg.backend.dto.UserDTO;
import com.tfg.backend.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class UserApiFunctionalITest extends BaseApiFunctionalITest {

    private static final String BASE_URL_USERS = "/api/v1/users";

    private User testUser;
    private Shop testShop;
    private Long testAddressId;
    private Long testCardId;

    private String adminCookie;
    private String userCookie;

    @BeforeEach
    public void setUp() {
        transactionTemplate.executeWithoutResult(status -> {
            Address shopAddress = new Address("ShopHQ", "Main Street", "1", "1A", "28000", "Madrid", "Spain");
            testShop = new Shop("API Test Shop", shopAddress, 5000.0);
            testShop.setReferenceCode("SHP-001");
            testShop = shopRepository.save(testShop);

            User testAdmin = new User("Admin", "admin_usr", "admin@usr.com", passwordEncoder.encode("pass"), "ADMIN");
            userRepository.save(testAdmin);

            User testDriver = new User("Driver", "driver_usr", "driver@usr.com", passwordEncoder.encode("pass"), "DRIVER");
            userRepository.save(testDriver);

            testUser = new User("User", "user_usr", "user@usr.com", passwordEncoder.encode("pass"), "USER");

            Address userAddress = new Address("Home", "User St", "5", "B", "28005", "Madrid", "Spain");
            testUser.getAddresses().add(userAddress);

            PaymentCard userCard = new PaymentCard("Personal Card", "User Name", "1234567812345678", "123", YearMonth.of(2028, 12));
            testUser.getCards().add(userCard);

            testUser = userRepository.saveAndFlush(testUser);

            testAddressId = testUser.getAddresses().getFirst().getId();
            testCardId = testUser.getCards().getFirst().getId();
        });

        adminCookie = loginAndGetCookie("admin_usr", "pass");
        userCookie = loginAndGetCookie("user_usr", "pass");
    }

    @Test
    public void getSessionInfo_AsUser_ReturnsUserLoginDTO() {
        given().spec(getSpec(BASE_URL_USERS, userCookie))
                .when().get("/session")
                .then().statusCode(200).body("username", equalTo("user_usr"));
    }

    @Test
    public void getLoggedUser_AsUser_ReturnsUserDTO() {
        given().spec(getSpec(BASE_URL_USERS, userCookie))
                .when().get("/me")
                .then().statusCode(200).body("addresses.size()", greaterThan(0));
    }

    @Test
    public void getAvailableDrivers_AsAdmin_ReturnsDriversList() {
        given().spec(getSpec(BASE_URL_USERS, adminCookie))
                .when().get("/drivers/available/")
                .then().statusCode(200).body("[0].username", equalTo("driver_usr"));
    }

    @Test
    public void setSelectedShop_AsUser_UpdatesSelection() {
        Map<String, Long> requestBody = new HashMap<>();
        requestBody.put("shopId", testShop.getId());

        given().spec(getSpec(BASE_URL_USERS, userCookie))
                .body(requestBody).when().put("/shop")
                .then().statusCode(200).body(equalTo("true"));
    }

    @Test
    public void updateLoggedUserData_AsUser_UpdatesInfo() {
        UserDTO updateData = new UserDTO();
        updateData.setName("Updated Name");
        updateData.setUsername("user_usr");
        updateData.setEmail("updated@usr.com");
        updateData.setPhone("123456789");

        given().spec(getSpec(BASE_URL_USERS, userCookie))
                .body(updateData).when().put("/data")
                .then().statusCode(200).body("name", equalTo("Updated Name"));
    }

    @Test
    public void createAddress_AsUser_CreatesAddress() {
        AddressDTO newAddress = new AddressDTO();
        newAddress.setAlias("Work");
        newAddress.setStreet("Work St");

        given().spec(getSpec(BASE_URL_USERS, userCookie))
                .body(newAddress).when().post("/addresses")
                .then().statusCode(201).body("addresses.alias", hasItem("Work"));
    }

    @Test
    public void deleteAddress_AsUser_RemovesAddress() {
        given().spec(getSpec(BASE_URL_USERS, userCookie))
                .pathParam("id", testAddressId).when().delete("/addresses/{id}")
                .then().statusCode(200).body("addresses.size()", equalTo(0));
    }

    @Test
    public void createPaymentCard_AsUser_CreatesCard() {
        PaymentCardDTO newCard = new PaymentCardDTO();
        newCard.setAlias("Business Card");
        newCard.setCardOwnerName("User Name");
        newCard.setNumber("1111222233334444");
        newCard.setCvv("999");
        newCard.setDueDate("05/29");

        given().spec(getSpec(BASE_URL_USERS, userCookie))
                .body(newCard).when().post("/cards")
                .then().statusCode(201).body("cards.alias", hasItem("Business Card"));
    }

    @Test
    public void toggleUserBanById_AsAdmin_ChangesBanState() {
        given().spec(getSpec(BASE_URL_USERS, adminCookie))
                .pathParam("id", testUser.getId()).body(true).when().put("/ban/{id}")
                .then().statusCode(200).body("banned", equalTo(true));
    }

    @Test
    public void anonymizeUserById_AsAdmin_AnonymizesData() {
        given().spec(getSpec(BASE_URL_USERS, adminCookie))
                .pathParam("id", testUser.getId()).when().put("/anon/{id}")
                .then().statusCode(200).body("deleted", equalTo(true)).body("email", containsString("deleteduser_"));
    }

    @Test
    public void deleteUserById_AsAdmin_DeletesUser() {
        given().spec(getSpec(BASE_URL_USERS, adminCookie))
                .pathParam("id", testUser.getId()).when().delete("/{id}")
                .then().statusCode(200).body(equalTo("true"));
    }

    @Test
    public void uploadUserImage_AsUser_UploadsMultipart() {
        given().spec(getSpec(BASE_URL_USERS, userCookie)).contentType("multipart/form-data")
                .pathParam("id", testUser.getId()).multiPart("image", "avatar.jpg", "fake_image".getBytes(), "image/jpeg")
                .when().put("/image/{id}")
                .then().statusCode(200).body("imageInfo", notNullValue());
    }
}