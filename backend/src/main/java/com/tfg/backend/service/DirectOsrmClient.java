package com.tfg.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tfg.backend.dto.RouteDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Component
@Profile({"local", "test", "default"})
@Slf4j
public class DirectOsrmClient implements OsrmClient {

    private final RestClient osrmRestClient;

    public DirectOsrmClient(@Qualifier("osrmRestClient") RestClient osrmRestClient) {
        this.osrmRestClient = osrmRestClient;
    }

    @Override
    public RouteDTO getRoute(double fromLat, double fromLng, double toLat, double toLng) {
        String path = String.format(java.util.Locale.US,
                "/route/v1/driving/%f,%f;%f,%f", fromLng, fromLat, toLng, toLat);
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
        List<List<Double>> coordinates = new ArrayList<>();
        JsonNode coordsNode = route.path("geometry").path("coordinates");
        if (coordsNode.isArray()) {
            for (JsonNode point : coordsNode) {
                if (point.isArray() && point.size() >= 2) {
                    coordinates.add(List.of(point.get(0).asDouble(), point.get(1).asDouble()));
                }
            }
        }
        return new RouteDTO(route.path("duration").asDouble(), route.path("distance").asDouble(), coordinates);
    }
}
