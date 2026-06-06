package com.tfg.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tfg.backend.dto.RouteDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Profile("prod")
@Slf4j
public class LambdaOsrmClient implements OsrmClient {

    private final LambdaClient lambdaClient;
    private final ObjectMapper objectMapper;
    private final String functionName;

    public LambdaOsrmClient(
            @Value("${lambda.osrm.function-name:frict-osrm}") String functionName,
            ObjectMapper objectMapper) {
        this.lambdaClient = LambdaClient.create();
        this.objectMapper = objectMapper;
        this.functionName = functionName;
    }

    @Override
    public RouteDTO getRoute(double fromLat, double fromLng, double toLat, double toLng) {
        try {
            String payload = objectMapper.writeValueAsString(
                    Map.of("fromLat", fromLat, "fromLng", fromLng, "toLat", toLat, "toLng", toLng));

            InvokeResponse response = lambdaClient.invoke(InvokeRequest.builder()
                    .functionName(functionName)
                    .payload(SdkBytes.fromUtf8String(payload))
                    .build());

            JsonNode result = objectMapper.readTree(response.payload().asByteArray());

            if (result.has("errorMessage")) {
                log.error("Lambda OSRM error: {}", result.get("errorMessage").asText());
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Routing provider error");
            }
            if (result.has("error")) {
                log.warn("OSRM Lambda returned no route: {}", result.get("error").asText());
                return null;
            }

            List<List<Double>> coordinates = new ArrayList<>();
            JsonNode coordsNode = result.path("coordinates");
            if (coordsNode.isArray()) {
                for (JsonNode point : coordsNode) {
                    if (point.isArray() && point.size() >= 2) {
                        coordinates.add(List.of(point.get(0).asDouble(), point.get(1).asDouble()));
                    }
                }
            }
            return new RouteDTO(
                    result.path("durationSeconds").asDouble(),
                    result.path("distanceMeters").asDouble(),
                    coordinates);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lambda OSRM routing failed", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Routing provider unreachable");
        }
    }
}
