package com.tfg.backend.unit;

import com.tfg.backend.dto.AddressDTO;
import com.tfg.backend.dto.CoordinatesDTO;
import com.tfg.backend.service.LocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class LocationServiceUTest {

    private MockRestServiceServer mockServer;
    private LocationService locationService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://nominatim.test");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient osrmClient = RestClient.builder().baseUrl("https://osrm.test").build();
        locationService = new LocationService(builder.build(), osrmClient);
    }

    @Nested
    @DisplayName("reverseGeocode")
    class ReverseGeocode {

        @Test
        @DisplayName("Maps Nominatim address fields, preferring road over pedestrian over street")
        void reverseGeocode_MapsAddressFields() {
            String responseJson = """
                    {
                      "address": {
                        "road": "Calle Gran Vía",
                        "house_number": "1",
                        "city": "Madrid",
                        "postcode": "28013",
                        "country": "España"
                      }
                    }
                    """;

            mockServer.expect(requestTo("https://nominatim.test/reverse?format=json&lat=40.4168&lon=-3.7038"))
                    .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

            AddressDTO result = locationService.reverseGeocode(40.4168, -3.7038);

            assertEquals("Calle Gran Vía", result.getStreet());
            assertEquals("1", result.getNumber());
            assertEquals("Madrid", result.getCity());
            assertEquals("28013", result.getPostalCode());
            assertEquals("España", result.getCountry());
            assertEquals(40.4168, result.getLatitude());
            assertEquals(-3.7038, result.getLongitude());
            mockServer.verify();
        }

        @Test
        @DisplayName("Falls back to pedestrian when road is missing, and to town when city is missing")
        void reverseGeocode_FallbackFields() {
            String responseJson = """
                    {
                      "address": {
                        "pedestrian": "Paseo Marítimo",
                        "town": "Sitges",
                        "postcode": "08870",
                        "country": "España"
                      }
                    }
                    """;

            mockServer.expect(requestTo("https://nominatim.test/reverse?format=json&lat=41.2364&lon=1.809"))
                    .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

            AddressDTO result = locationService.reverseGeocode(41.2364, 1.809);

            assertEquals("Paseo Marítimo", result.getStreet());
            assertEquals("", result.getNumber());
            assertEquals("Sitges", result.getCity());
        }

        @Test
        @DisplayName("Returns null when Nominatim returns no address")
        void reverseGeocode_NotFound_ReturnsNull() {
            mockServer.expect(requestTo("https://nominatim.test/reverse?format=json&lat=0.0&lon=0.0"))
                    .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

            assertNull(locationService.reverseGeocode(0.0, 0.0));
        }

        @Test
        @DisplayName("Throws 502 when Nominatim returns 5xx error")
        void reverseGeocode_ProviderError() {
            mockServer.expect(requestTo("https://nominatim.test/reverse?format=json&lat=1.0&lon=1.0"))
                    .andRespond(withServerError());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> locationService.reverseGeocode(1.0, 1.0));

            assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatusCode());
        }
    }

    @Nested
    @DisplayName("directGeocode")
    class DirectGeocode {

        @Test
        @DisplayName("Builds query string from non-blank address fields and returns first result coordinates")
        void directGeocode_ReturnsCoordinates() {
            String responseJson = """
                    [
                      {"lat": "40.4168", "lon": "-3.7038", "display_name": "Madrid, Spain"}
                    ]
                    """;

            mockServer.expect(requestTo("https://nominatim.test/search?q=Calle%20Gran%20V%C3%ADa%201,%20Madrid,%2028013,%20Spain&format=json&limit=1"))
                    .andExpect(method(org.springframework.http.HttpMethod.GET))
                    .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

            AddressDTO input = new AddressDTO();
            input.setStreet("Calle Gran Vía");
            input.setNumber("1");
            input.setCity("Madrid");
            input.setPostalCode("28013");
            input.setCountry("Spain");

            CoordinatesDTO result = locationService.directGeocode(input);

            assertEquals(40.4168, result.getLatitude());
            assertEquals(-3.7038, result.getLongitude());
            mockServer.verify();
        }

        @Test
        @DisplayName("Skips blank fields when building the query")
        void directGeocode_SkipsBlankFields() {
            String responseJson = """
                    [
                      {"lat": "40.0", "lon": "-3.0"}
                    ]
                    """;

            mockServer.expect(requestTo("https://nominatim.test/search?q=Madrid,%20Spain&format=json&limit=1"))
                    .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

            AddressDTO input = new AddressDTO();
            input.setCity("Madrid");
            input.setCountry("Spain");

            CoordinatesDTO result = locationService.directGeocode(input);

            assertEquals(40.0, result.getLatitude());
            assertEquals(-3.0, result.getLongitude());
        }

        @Test
        @DisplayName("Throws 400 when address is null or has no usable fields")
        void directGeocode_EmptyQuery() {
            AddressDTO empty = new AddressDTO();
            ResponseStatusException exFromEmpty = assertThrows(ResponseStatusException.class,
                    () -> locationService.directGeocode(empty));
            assertEquals(HttpStatus.BAD_REQUEST, exFromEmpty.getStatusCode());

            ResponseStatusException exFromNull = assertThrows(ResponseStatusException.class,
                    () -> locationService.directGeocode(null));
            assertEquals(HttpStatus.BAD_REQUEST, exFromNull.getStatusCode());
        }

        @Test
        @DisplayName("Returns null when Nominatim returns an empty array")
        void directGeocode_NotFound_ReturnsNull() {
            mockServer.expect(requestTo("https://nominatim.test/search?q=Nowhere&format=json&limit=1"))
                    .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

            AddressDTO input = new AddressDTO();
            input.setCity("Nowhere");

            assertNull(locationService.directGeocode(input));
        }
    }
}
