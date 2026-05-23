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

    private final GeocodingClient geocodingClient;
    private final RestClient osrmRestClient;

    public LocationService(GeocodingClient geocodingClient,
                           @Qualifier("osrmRestClient") RestClient osrmRestClient) {
        this.geocodingClient = geocodingClient;
        this.osrmRestClient = osrmRestClient;
    }

    public AddressDTO reverseGeocode(double latitude, double longitude) {
        return geocodingClient.reverseGeocode(latitude, longitude);
    }

    public CoordinatesDTO directGeocode(AddressDTO address) {
        return geocodingClient.directGeocode(address);
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
}
