package com.tfg.backend.api;

import com.tfg.backend.dto.AddressDTO;
import com.tfg.backend.dto.ShopDTO;
import com.tfg.backend.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class ShopApiFunctionalITest extends BaseApiFunctionalITest {

    private static final String BASE_URL_SHOPS = "/api/v1/shops";

    private User testUser;
    private Shop testShop;
    private Truck testTruck;
    private ShopStock testStock;

    private String adminCookie;
    private String managerCookie;
    private String driverCookie;
    private String userCookie;

    @BeforeEach
    public void setUp() {
        transactionTemplate.executeWithoutResult(status -> {
            Category category = new Category("Otros", "icon", "banner", "Desc", "Desc");
            categoryRepository.saveAndFlush(category);

            Product testProduct = new Product("API Shop Product", "Desc", 10.0, 5.0);
            testProduct.setReferenceCode("REF-SHP-PROD");
            testProduct.setActive(true);
            testProduct = productRepository.saveAndFlush(testProduct);

            Address shopAddress = new Address("ShopHQ", "Main Street", "1", "1A", "28000", "Madrid", "Spain");
            testShop = new Shop("API Test Shop", shopAddress, 5000.0);
            testShop.setReferenceCode("SHP-001");
            testShop = shopRepository.saveAndFlush(testShop);

            testStock = new ShopStock(testShop, testProduct, 50);
            testStock = shopStockRepository.saveAndFlush(testStock);

            User testAdmin = new User("Admin", "admin_shp", "admin@shp.com", passwordEncoder.encode("pass"), "ADMIN");
            userRepository.saveAndFlush(testAdmin);

            User testManager = new User("Manager", "manager_shp", "manager@shp.com", passwordEncoder.encode("pass"), "MANAGER");
            userRepository.saveAndFlush(testManager);

            User testDriver = new User("Driver", "driver_shp", "driver@shp.com", passwordEncoder.encode("pass"), "DRIVER");
            userRepository.saveAndFlush(testDriver);

            testUser = new User("User", "user_shp", "user@shp.com", passwordEncoder.encode("pass"), "USER");
            userRepository.saveAndFlush(testUser);

            testShop.setAssignedManager(testManager);
            testShop = shopRepository.saveAndFlush(testShop);

            Address truckAddress = new Address("Garage", "Industrial St", "10", "B", "28001", "Madrid", "Spain");
            testTruck = new Truck("1234-ABC", truckAddress, 25);
            testTruck = truckRepository.saveAndFlush(testTruck);
            testTruck.setAssignedDriver(testDriver);
            testTruck.setAssignedShop(testShop);
            testTruck = truckRepository.saveAndFlush(testTruck);
        });

        adminCookie = loginAndGetCookie("admin_shp", "pass");
        managerCookie = loginAndGetCookie("manager_shp", "pass");
        driverCookie = loginAndGetCookie("driver_shp", "pass");
        userCookie = loginAndGetCookie("user_shp", "pass");
    }

    @Test
    public void getAllShopsPage_AsAdmin_ReturnsPagedShops() {
        given().spec(getSpec(BASE_URL_SHOPS, adminCookie))
                .when().get("/")
                .then().statusCode(200).body("items", notNullValue());
    }

    @Test
    public void getAssignedShopsPage_AsManager_ReturnsPagedShops() {
        given().spec(getSpec(BASE_URL_SHOPS, managerCookie))
                .when().get()
                .then().statusCode(200).body("items[0].id", equalTo(testShop.getId().intValue()));
    }

    @Test
    public void getAllShopsList_AsUser_ReturnsList() {
        given().spec(getSpec(BASE_URL_SHOPS, userCookie))
                .when().get("/list")
                .then().statusCode(200).body("[0].name", equalTo("API Test Shop"));
    }

    @Test
    public void getShopByAssignedTruckId_AsDriver_ReturnsShop() {
        given().spec(getSpec(BASE_URL_SHOPS, driverCookie))
                .pathParam("id", testTruck.getId())
                .when().get("/truck/{id}")
                .then().statusCode(200).body("id", equalTo(testShop.getId().intValue()));
    }

    @Test
    public void getShopStocks_AsManager_ReturnsPagedStocks() {
        given().spec(getSpec(BASE_URL_SHOPS, managerCookie))
                .pathParam("id", testShop.getId())
                .when().get("/stock/{id}")
                .then().statusCode(200).body("items[0].units", equalTo(50));
    }

    @Test
    public void createShop_AsAdmin_CreatesShopWithAddress() {
        ShopDTO newShop = new ShopDTO();
        newShop.setName("New Madrid Shop");
        newShop.setReferenceCode("SHP-MAD-01");
        newShop.setAssignedBudget(10000.0);
        newShop.setAddress(new AddressDTO(new Address("Alias", "New Street", "2", "2", "28000", "Madrid", "Spain")));

        given().spec(getSpec(BASE_URL_SHOPS, adminCookie))
                .body(newShop).when().post()
                .then().statusCode(201).body("name", equalTo("New Madrid Shop"));
    }

    @Test
    public void updateShop_AsManager_UpdatesShopAndAddress() {
        ShopDTO updateShop = new ShopDTO();
        updateShop.setName("Updated Shop Name");
        updateShop.setAssignedBudget(7500.0);
        updateShop.setAddress(new AddressDTO(new Address("Alias", "Updated St", "3", "3", "28000", "Madrid", "Spain")));

        given().spec(getSpec(BASE_URL_SHOPS, managerCookie))
                .pathParam("id", testShop.getId()).body(updateShop).when().put("/{id}")
                .then().statusCode(202).body("name", equalTo("Updated Shop Name"));
    }

    @Test
    public void setAssignedTruck_AsManager_UpdatesAssignment() {
        given().spec(getSpec(BASE_URL_SHOPS, managerCookie))
                .pathParam("shopId", testShop.getId()).pathParam("truckId", testTruck.getId()).queryParam("state", true)
                .when().put("/{shopId}/assign/truck/{truckId}")
                .then().statusCode(200).body("plateNumber", equalTo("1234-ABC"));
    }

    @Test
    public void setAssignedManager_AsAdmin_UpdatesAssignment() {
        given().spec(getSpec(BASE_URL_SHOPS, adminCookie))
                .pathParam("shopId", testShop.getId()).pathParam("userId", testUser.getId()).queryParam("state", true)
                .when().put("/{shopId}/assign/manager/{userId}")
                .then().statusCode(200).body("assignedManager.id", equalTo(testUser.getId().intValue()));
    }

    @Test
    public void restockProduct_AsManager_AddsUnitsToStock() {
        given().spec(getSpec(BASE_URL_SHOPS, managerCookie))
                .pathParam("stockId", testStock.getId()).queryParam("units", 25)
                .when().put("/restock/{stockId}")
                .then().statusCode(200).body("units", equalTo(75));
    }

    @Test
    public void toggleLocalActivation_AsManager_ChangesStockState() {
        given().spec(getSpec(BASE_URL_SHOPS, managerCookie))
                .pathParam("id", testStock.getId()).queryParam("state", false)
                .when().put("/active/{id}").then().statusCode(200);
    }

    @Test
    public void deleteShop_AsAdmin_DeletesShop() {
        given().spec(getSpec(BASE_URL_SHOPS, adminCookie))
                .pathParam("id", testShop.getId()).when().delete("/{id}").then().statusCode(200);
    }

    @Test
    public void uploadShopImage_AsAdmin_UploadsMultipart() {
        given().spec(getSpec(BASE_URL_SHOPS, adminCookie))
                .contentType("multipart/form-data")
                .pathParam("id", testShop.getId())
                .multiPart("image", "shop_front.jpg", "fake_image_content".getBytes(), "image/jpeg")
                .when().put("/image/{id}")
                .then().statusCode(200).body("imageInfo", notNullValue());
    }
}