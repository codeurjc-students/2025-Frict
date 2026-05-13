package com.tfg.backend.api;

import com.tfg.backend.dto.AddressDTO;
import com.tfg.backend.dto.CoordinatesDTO;
import com.tfg.backend.model.User;
import com.tfg.backend.service.LocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

public class LocationApiFunctionalITest extends BaseApiFunctionalITest {

    private static final String BASE_URL_LOCATIONS = "/api/v1/locations";

    @MockitoBean
    private LocationService locationService;

    private String adminCookie;
    private String managerCookie;
    private String driverCookie;
    private String userCookie;

    @BeforeEach
    public void setUp() {
        transactionTemplate.executeWithoutResult(status -> {
            userRepository.saveAndFlush(new User("Admin", "admin_loc", "admin@loc.com", passwordEncoder.encode("pass"), "ADMIN"));
            userRepository.saveAndFlush(new User("Manager", "manager_loc", "manager@loc.com", passwordEncoder.encode("pass"), "MANAGER"));
            userRepository.saveAndFlush(new User("Driver", "driver_loc", "driver@loc.com", passwordEncoder.encode("pass"), "DRIVER"));
            userRepository.saveAndFlush(new User("User", "user_loc", "user@loc.com", passwordEncoder.encode("pass"), "USER"));
        });

        adminCookie = loginAndGetCookie("admin_loc", "pass");
        managerCookie = loginAndGetCookie("manager_loc", "pass");
        driverCookie = loginAndGetCookie("driver_loc", "pass");
        userCookie = loginAndGetCookie("user_loc", "pass");
    }

    // --- REVERSE ---

    @Test
    public void reverseGeocode_AsAdmin_Returns200WithAddress() {
        AddressDTO mocked = new AddressDTO();
        mocked.setStreet("Gran Vía");
        mocked.setCity("Madrid");
        mocked.setPostalCode("28013");
        mocked.setCountry("España");
        when(locationService.reverseGeocode(anyDouble(), anyDouble())).thenReturn(mocked);

        given().spec(getSpec(BASE_URL_LOCATIONS, adminCookie))
                .queryParam("lat", 40.4168)
                .queryParam("lng", -3.7038)
                .when().get("/reverse")
                .then().statusCode(200)
                .body("street", equalTo("Gran Vía"))
                .body("city", equalTo("Madrid"));
    }

    @Test
    public void reverseGeocode_AsManager_Returns200() {
        when(locationService.reverseGeocode(anyDouble(), anyDouble())).thenReturn(new AddressDTO());

        given().spec(getSpec(BASE_URL_LOCATIONS, managerCookie))
                .queryParam("lat", 40.0)
                .queryParam("lng", -3.0)
                .when().get("/reverse")
                .then().statusCode(200);
    }

    @Test
    public void reverseGeocode_AsDriver_Returns403() {
        given().spec(getSpec(BASE_URL_LOCATIONS, driverCookie))
                .queryParam("lat", 40.0)
                .queryParam("lng", -3.0)
                .when().get("/reverse")
                .then().statusCode(403);
    }

    @Test
    public void reverseGeocode_AsUser_Returns403() {
        given().spec(getSpec(BASE_URL_LOCATIONS, userCookie))
                .queryParam("lat", 40.0)
                .queryParam("lng", -3.0)
                .when().get("/reverse")
                .then().statusCode(403);
    }

    @Test
    public void reverseGeocode_Anonymous_Returns401() {
        given().spec(getSpec(BASE_URL_LOCATIONS, null))
                .queryParam("lat", 40.0)
                .queryParam("lng", -3.0)
                .when().get("/reverse")
                .then().statusCode(401);
    }

    // --- DIRECT ---

    @Test
    public void directGeocode_AsAdmin_Returns200WithCoordinates() {
        when(locationService.directGeocode(any(AddressDTO.class))).thenReturn(new CoordinatesDTO(40.4168, -3.7038));

        AddressDTO input = new AddressDTO();
        input.setStreet("Gran Vía");
        input.setCity("Madrid");
        input.setCountry("Spain");

        given().spec(getSpec(BASE_URL_LOCATIONS, adminCookie))
                .body(input)
                .when().post("/direct")
                .then().statusCode(200)
                .body("latitude", equalTo(40.4168f))
                .body("longitude", equalTo(-3.7038f));
    }

    @Test
    public void directGeocode_AsManager_Returns200() {
        when(locationService.directGeocode(any(AddressDTO.class))).thenReturn(new CoordinatesDTO(0.0, 0.0));

        AddressDTO input = new AddressDTO();
        input.setCity("Madrid");

        given().spec(getSpec(BASE_URL_LOCATIONS, managerCookie))
                .body(input)
                .when().post("/direct")
                .then().statusCode(200);
    }

    @Test
    public void directGeocode_AsUser_Returns403() {
        AddressDTO input = new AddressDTO();
        input.setCity("Madrid");

        given().spec(getSpec(BASE_URL_LOCATIONS, userCookie))
                .body(input)
                .when().post("/direct")
                .then().statusCode(403);
    }

    @Test
    public void directGeocode_Anonymous_Returns401() {
        AddressDTO input = new AddressDTO();
        input.setCity("Madrid");

        given().spec(getSpec(BASE_URL_LOCATIONS, null))
                .body(input)
                .when().post("/direct")
                .then().statusCode(401);
    }
}
