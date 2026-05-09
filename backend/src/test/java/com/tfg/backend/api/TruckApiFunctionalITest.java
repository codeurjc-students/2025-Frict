package com.tfg.backend.api;

import com.tfg.backend.dto.AddressDTO;
import com.tfg.backend.dto.TruckDTO;
import com.tfg.backend.model.Address;
import com.tfg.backend.model.Shop;
import com.tfg.backend.model.Truck;
import com.tfg.backend.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TruckApiFunctionalITest extends BaseApiFunctionalITest {

    private static final String BASE_URL_TRUCKS = "/api/v1/trucks";

    private User testDriver;
    private Shop testShop;
    private Truck testTruck;

    private String adminCookie;
    private String managerCookie;
    private String driverCookie;
    private String userCookie;

    @BeforeEach
    public void setUp() {
        transactionTemplate.executeWithoutResult(status -> {
            Address shopAddress = new Address("ShopHQ", "Main Street", "1", "1A", "28000", "Madrid", "Spain");
            testShop = new Shop("API Test Shop", shopAddress, 5000.0);
            testShop.setReferenceCode("SHP-001");
            testShop = shopRepository.save(testShop);

            User testAdmin = new User("Admin", "admin_trk", "admin@trk.com", passwordEncoder.encode("pass"), "ADMIN");
            userRepository.save(testAdmin);

            User testManager = new User("Manager", "manager_trk", "manager@trk.com", passwordEncoder.encode("pass"), "MANAGER");
            userRepository.save(testManager);

            testDriver = new User("Driver", "driver_trk", "driver@trk.com", passwordEncoder.encode("pass"), "DRIVER");
            userRepository.save(testDriver);

            User testUser = new User("User", "user_trk", "user@trk.com", passwordEncoder.encode("pass"), "USER");
            userRepository.save(testUser);

            testShop.setAssignedManager(testManager);
            testShop = shopRepository.save(testShop);

            Address truckAddress1 = new Address("Garage 1", "Industrial St", "10", "B", "28001", "Madrid", "Spain");
            testTruck = new Truck("1111-ABC", truckAddress1, 50);
            testTruck.setAssignedShop(testShop);
            testTruck.setAssignedDriver(testDriver);
            testTruck = truckRepository.save(testTruck);

            Address truckAddress2 = new Address("Garage 2", "Secondary St", "5", "A", "28002", "Madrid", "Spain");
            Truck unassignedTruck = new Truck("2222-XYZ", truckAddress2, 30);
            truckRepository.save(unassignedTruck);
        });

        adminCookie = loginAndGetCookie("admin_trk", "pass");
        managerCookie = loginAndGetCookie("manager_trk", "pass");
        driverCookie = loginAndGetCookie("driver_trk", "pass");
        userCookie = loginAndGetCookie("user_trk", "pass");
    }

    @Test
    public void getAllTrucksPage_AsAdmin_ReturnsPagedTrucks() {
        given().spec(getSpec(BASE_URL_TRUCKS, adminCookie))
                .when().get("/")
                .then().statusCode(200).body("items.size()", greaterThanOrEqualTo(2));
    }

    @Test
    public void getTruckById_AsManager_ReturnsTruck() {
        given().spec(getSpec(BASE_URL_TRUCKS, managerCookie))
                .pathParam("id", testTruck.getId())
                .when().get("/{id}")
                .then().statusCode(200).body("plateNumber", equalTo("1111-ABC"));
    }

    @Test
    public void getAssignedTruckByDriverId_AsDriver_ReturnsTruck() {
        given().spec(getSpec(BASE_URL_TRUCKS, driverCookie))
                .pathParam("driverId", testDriver.getId())
                .when().get("/user/{driverId}")
                .then().statusCode(200).body("plateNumber", equalTo("1111-ABC"));
    }

    @Test
    public void getAllShopTrucks_AsManager_ReturnsList() {
        given().spec(getSpec(BASE_URL_TRUCKS, managerCookie))
                .pathParam("shopId", testShop.getId())
                .when().get("/shop/{shopId}/list")
                .then().statusCode(200).body("[0].plateNumber", equalTo("1111-ABC"));
    }

    @Test
    public void getTrucksByShopId_AsUser_ReturnsPagedTrucks() {
        given().spec(getSpec(BASE_URL_TRUCKS, userCookie))
                .pathParam("shopId", testShop.getId())
                .when().get("/shop/{shopId}")
                .then().statusCode(200).body("items[0].plateNumber", equalTo("1111-ABC"));
    }

    @Test
    public void getAllUnassignedTrucks_AsAdmin_ReturnsAvailableList() {
        given().spec(getSpec(BASE_URL_TRUCKS, adminCookie))
                .when().get("/available/")
                .then().statusCode(200).body("[0].plateNumber", equalTo("2222-XYZ"));
    }

    @Test
    public void createTruck_AsAdmin_CreatesTruck() {
        TruckDTO newTruck = new TruckDTO();
        newTruck.setPlateNumber("9999-NEW");
        newTruck.setMaxOrderCapacity(100);
        newTruck.setAddress(new AddressDTO(new Address("Depot", "Central St", "10", "A", "28005", "Madrid", "Spain")));

        given().spec(getSpec(BASE_URL_TRUCKS, adminCookie))
                .body(newTruck).when().post()
                .then().statusCode(201).body("plateNumber", equalTo("9999-NEW"));
    }

    @Test
    public void updateTruck_AsAdmin_UpdatesTruckData() {
        TruckDTO updateData = given().spec(getSpec(BASE_URL_TRUCKS, adminCookie))
                .pathParam("id", testTruck.getId())
                .when().get("/{id}")
                .then().statusCode(200).extract().as(TruckDTO.class);

        updateData.setPlateNumber("1111-MOD");
        updateData.setMaxOrderCapacity(60);
        updateData.setAddress(new AddressDTO(new Address("Depot Mod", "Mod St", "2", "B", "28005", "Madrid", "Spain")));

        given().spec(getSpec(BASE_URL_TRUCKS, adminCookie))
                .pathParam("id", testTruck.getId()).body(updateData)
                .when().put("/{id}")
                .then().statusCode(202).body("plateNumber", equalTo("1111-MOD"));
    }

    @Test
    public void setAssignedDriver_AsAdmin_UpdatesAssignment() {
        given().spec(getSpec(BASE_URL_TRUCKS, adminCookie))
                .pathParam("truckId", testTruck.getId()).pathParam("driverId", testDriver.getId()).queryParam("state", false)
                .when().put("/{truckId}/assign/driver/{driverId}")
                .then().statusCode(200);

        given().spec(getSpec(BASE_URL_TRUCKS, adminCookie))
                .pathParam("truckId", testTruck.getId()).pathParam("driverId", testDriver.getId()).queryParam("state", true)
                .when().put("/{truckId}/assign/driver/{driverId}")
                .then().statusCode(200).body("assignedDriver.id", equalTo(testDriver.getId().intValue()));
    }

    @Test
    public void commentAndOrUpdateTruckStatus_AsAdmin_UpdatesStatus() {
        String responseBody = given().spec(getSpec(BASE_URL_TRUCKS, adminCookie))
                .pathParam("id", testTruck.getId())
                .queryParam("truckStatus", "MAINTENANCE").queryParam("comment", "Going to the mechanic")
                .when().put("/status/{id}")
                .then().statusCode(200).extract().asString();

        assertTrue(responseBody.contains("Going to the mechanic"), "The truck history should contain the new status comment");
    }

    @Test
    public void deleteTruck_AsAdmin_DeletesTruck() {
        given().spec(getSpec(BASE_URL_TRUCKS, adminCookie))
                .pathParam("id", testTruck.getId()).when().delete("/{id}")
                .then().statusCode(200);

        given().spec(getSpec(BASE_URL_TRUCKS, adminCookie))
                .pathParam("id", testTruck.getId()).when().get("/{id}")
                .then().statusCode(404);
    }
}