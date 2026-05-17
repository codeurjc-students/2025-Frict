package com.tfg.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tfg.backend.dto.AddressDTO;
import com.tfg.backend.dto.CoordinatesDTO;
import com.tfg.backend.dto.RouteDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class LocationService {

    private final RestClient nominatimRestClient;
    private final RestClient osrmRestClient;

    public LocationService(@Qualifier("nominatimRestClient") RestClient nominatimRestClient,
                           @Qualifier("osrmRestClient") RestClient osrmRestClient) {
        this.nominatimRestClient = nominatimRestClient;
        this.osrmRestClient = osrmRestClient;
    }

    public AddressDTO reverseGeocode(double latitude, double longitude) {
        JsonNode response;
        try {
            response = nominatimRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/reverse")
                            .queryParam("format", "json")
                            .queryParam("lat", latitude)
                            .queryParam("lon", longitude)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            log.error("Nominatim reverse geocoding call failed", ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Geocoding provider unreachable");
        }

        if (response == null || !response.hasNonNull("address")) {
            return null;
        }

        JsonNode addr = response.get("address");

        AddressDTO dto = new AddressDTO();
        dto.setStreet(firstNonBlank(
                addr.path("road").asText(""),
                addr.path("pedestrian").asText(""),
                addr.path("street").asText("")));
        dto.setNumber(addr.path("house_number").asText(""));
        dto.setCity(firstNonBlank(
                addr.path("city").asText(""),
                addr.path("town").asText(""),
                addr.path("village").asText("")));
        dto.setPostalCode(addr.path("postcode").asText(""));
        dto.setCountry(addr.path("country").asText(""));
        dto.setLatitude(latitude);
        dto.setLongitude(longitude);
        return dto;
    }

    public CoordinatesDTO directGeocode(AddressDTO address) {
        String query = buildQuery(address);
        if (query.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Address query is empty");
        }

        JsonNode response;
        try {
            response = nominatimRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("q", query)
                            .queryParam("format", "json")
                            .queryParam("limit", 1)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            log.error("Nominatim direct geocoding call failed", ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Geocoding provider unreachable");
        }

        if (response == null || !response.isArray() || response.isEmpty()) {
            return null;
        }

        JsonNode first = response.get(0);
        try {
            double lat = Double.parseDouble(first.path("lat").asText());
            double lon = Double.parseDouble(first.path("lon").asText());
            return new CoordinatesDTO(lat, lon);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid coordinates returned by geocoding provider");
        }
    }

    private String buildQuery(AddressDTO address) {
        if (address == null) return "";

        String streetAndNumber = String.join(" ",
                nullSafe(address.getStreet()),
                nullSafe(address.getNumber())).trim();

        List<String> parts = new ArrayList<>();
        if (!streetAndNumber.isBlank()) parts.add(streetAndNumber);
        addIfNotBlank(parts, address.getCity());
        addIfNotBlank(parts, address.getPostalCode());
        addIfNotBlank(parts, address.getCountry());

        return String.join(", ", parts);
    }

    private static void addIfNotBlank(List<String> parts, String value) {
        if (value != null && !value.isBlank()) parts.add(value);
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    public RouteDTO getRoute(double fromLat, double fromLng, double toLat, double toLng) {
        String path = String.format(java.util.Locale.US, "/route/v1/driving/%f,%f;%f,%f", fromLng, fromLat, toLng, toLat);
        JsonNode response;
        try {
            response = osrmRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(path)
                            .queryParam("overview", "full")
                            .queryParam("geometries", "geojson")
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            log.error("OSRM routing call failed", ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Routing provider unreachable");
        }

        if (response == null || !response.hasNonNull("routes") || response.get("routes").isEmpty()) {
            return null;
        }

        JsonNode route = response.get("routes").get(0);
        double duration = route.path("duration").asDouble();
        double distance = route.path("distance").asDouble();

        List<List<Double>> coordinates = new ArrayList<>();
        JsonNode coordsNode = route.path("geometry").path("coordinates");
        if (coordsNode.isArray()) {
            for (JsonNode point : coordsNode) {
                if (point.isArray() && point.size() >= 2) {
                    coordinates.add(List.of(point.get(0).asDouble(), point.get(1).asDouble()));
                }
            }
        }
        return new RouteDTO(duration, distance, coordinates);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return "";
    }
}
