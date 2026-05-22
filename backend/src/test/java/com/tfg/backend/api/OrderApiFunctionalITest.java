package com.tfg.backend.api;

import com.tfg.backend.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class OrderApiFunctionalITest extends BaseApiFunctionalITest {

    private static final String BASE_URL_ORDERS = "/api/v1/orders";

    private Product testProduct;
    private Truck testTruck;

    private Long addressId;
    private Long cardId;

    private String adminCookie;
    private String userCookie;
    private String hackerCookie;

    @BeforeEach
    public void setUp() {
        transactionTemplate.executeWithoutResult(status -> {
            testProduct = new Product("Test Product", "Desc", 10.0, 5.0);
            testProduct.setReferenceCode("REF-1234");
            testProduct = productRepository.saveAndFlush(testProduct);

            Shop testShop = new Shop("Test Shop", new Address("Shop", "Shop St", "1", "1", "00000", "City", "Country"), 5000.0);
            testShop = shopRepository.saveAndFlush(testShop);

            User testAdmin = new User("Admin", "admin_ord", "admin@test.com", passwordEncoder.encode("pass"), "ADMIN");
            testAdmin = userRepository.saveAndFlush(testAdmin);

            ShopStock stock = new ShopStock(testShop, testProduct, 5);
            shopStockRepository.saveAndFlush(stock);

            Address truckAddress = new Address("Garage", "Ind St", "1", "1", "28001", "City", "Country");
            testTruck = new Truck("1111-ORD", truckAddress, 20);
            testTruck = truckRepository.saveAndFlush(testTruck);
            testTruck.setAssignedShop(testShop);
            testTruck = truckRepository.saveAndFlush(testTruck);

            User testUser = new User("Buyer User", "buyer_user", "buyer@test.com", passwordEncoder.encode("pass"), "USER");
            testUser.setSelectedShop(testShop);

            Address address = new Address("Home", "Fake St", "123", "1A", "00000", "City", "Country");
            testUser.getAddresses().add(address);

            PaymentCard card = new PaymentCard("My Card", "Buyer User", "1111222233334444", "123", YearMonth.now().plusYears(1));
            testUser.getCards().add(card);

            testUser = userRepository.saveAndFlush(testUser);
            addressId = testUser.getAddresses().iterator().next().getId();
            cardId = testUser.getCards().iterator().next().getId();

            User hackerUser = new User("Hacker", "hacker", "hacker@test.com", passwordEncoder.encode("pass"), "USER");
            userRepository.saveAndFlush(hackerUser);
        });

        adminCookie = loginAndGetCookie("admin_ord", "pass");
        userCookie = loginAndGetCookie("buyer_user", "pass");
        hackerCookie = loginAndGetCookie("hacker", "pass");
    }

    @Test
    public void updateItemQuantity_InCart_UpdatesCorrectly() {
        given().spec(getSpec(BASE_URL_ORDERS, userCookie)).pathParam("id", testProduct.getId()).queryParam("quantity", 1).when().post("/cart/{id}");

        given().spec(getSpec(BASE_URL_ORDERS, userCookie))
                .pathParam("id", testProduct.getId()).queryParam("quantity", 3)
                .when().put("/cart/{id}").then().statusCode(200);

        given().spec(getSpec(BASE_URL_ORDERS, userCookie))
                .when().get("/cart/summary")
                .then().statusCode(200).body("totalItems", equalTo(3)).body("totalCost", equalTo(30.0f));
    }

    @Test
    public void clearCartItems_RemovesAllItems() {
        given().spec(getSpec(BASE_URL_ORDERS, userCookie)).pathParam("id", testProduct.getId()).queryParam("quantity", 2).when().post("/cart/{id}");

        given().spec(getSpec(BASE_URL_ORDERS, userCookie))
                .when().delete("/cart")
                .then().statusCode(200).body("totalItems", equalTo(0));
    }

    @Test
    public void cartLifecycleTest() {
        Long orderItemId = given().spec(getSpec(BASE_URL_ORDERS, userCookie))
                .pathParam("id", testProduct.getId()).queryParam("quantity", 2)
                .when().post("/cart/{id}")
                .then().statusCode(201).extract().jsonPath().getLong("id");

        given().spec(getSpec(BASE_URL_ORDERS, userCookie))
                .when().get("/cart/summary")
                .then().statusCode(200).body("totalItems", equalTo(2)).body("totalCost", equalTo(20.0f));

        given().spec(getSpec(BASE_URL_ORDERS, userCookie))
                .pathParam("itemId", orderItemId)
                .when().delete("/cart/{itemId}")
                .then().statusCode(200).body("totalItems", equalTo(0));
    }

    @Test
    public void createOrderTest() {
        given().spec(getSpec(BASE_URL_ORDERS, userCookie)).pathParam("id", testProduct.getId()).queryParam("quantity", 1).when().post("/cart/{id}");

        given().spec(getSpec(BASE_URL_ORDERS, userCookie))
                .queryParam("addressId", addressId).queryParam("cardId", cardId)
                .when().post()
                .then().statusCode(201).body("totalCost", equalTo(10.0f));
    }

    @Test
    public void cancelOrder_ForbiddenForOtherUsers() {
        given().spec(getSpec(BASE_URL_ORDERS, userCookie)).pathParam("id", testProduct.getId()).queryParam("quantity", 1).when().post("/cart/{id}");
        Long orderId = given().spec(getSpec(BASE_URL_ORDERS, userCookie)).queryParam("addressId", addressId).queryParam("cardId", cardId)
                .when().post().jsonPath().getLong("id");

        given().spec(getSpec(BASE_URL_ORDERS, hackerCookie))
                .pathParam("id", orderId)
                .when().put("/cancel/{id}")
                .then().statusCode(403);
    }

    @Test
    public void getOrdersPage_AsUser_ReturnsList() {
        given().spec(getSpec(BASE_URL_ORDERS, userCookie)).pathParam("id", testProduct.getId()).queryParam("quantity", 1).when().post("/cart/{id}");
        given().spec(getSpec(BASE_URL_ORDERS, userCookie)).queryParam("addressId", addressId).queryParam("cardId", cardId).when().post();

        given().spec(getSpec(BASE_URL_ORDERS, userCookie))
                .when().get()
                .then().statusCode(200).body("items", notNullValue()).body("items.size()", greaterThan(0));
    }

    @Test
    public void assignTruckToOrder_AsAdmin_UpdatesAssignment() {
        given().spec(getSpec(BASE_URL_ORDERS, userCookie)).pathParam("id", testProduct.getId()).queryParam("quantity", 1).when().post("/cart/{id}");
        Long orderId = given().spec(getSpec(BASE_URL_ORDERS, userCookie)).queryParam("addressId", addressId).queryParam("cardId", cardId)
                .when().post().jsonPath().getLong("id");

        given().spec(getSpec(BASE_URL_ORDERS, adminCookie))
                .pathParam("orderId", orderId).pathParam("truckId", testTruck.getId()).queryParam("state", true)
                .when().post("/{orderId}/assign/truck/{truckId}")
                .then().statusCode(200).body("assignedTruckId", equalTo(testTruck.getId().intValue()));
    }

    @Test
    public void updateOrderStatus_AsAdmin_ChangesStatusSuccessfully() {
        given().spec(getSpec(BASE_URL_ORDERS, userCookie)).pathParam("id", testProduct.getId()).queryParam("quantity", 1).when().post("/cart/{id}");
        Long orderId = given().spec(getSpec(BASE_URL_ORDERS, userCookie)).queryParam("addressId", addressId).queryParam("cardId", cardId)
                .when().post().jsonPath().getLong("id");

        given().spec(getSpec(BASE_URL_ORDERS, adminCookie))
                .pathParam("id", orderId).queryParam("orderStatus", "SENT").queryParam("comment", "Order has been dispatched")
                .when().put("/{id}")
                .then().statusCode(200)
                .body("history[-1].status", equalTo("Enviado"));
    }
}