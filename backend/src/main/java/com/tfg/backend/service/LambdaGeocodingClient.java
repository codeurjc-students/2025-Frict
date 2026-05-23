package com.tfg.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tfg.backend.dto.AddressDTO;
import com.tfg.backend.dto.CoordinatesDTO;
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

import java.util.Map;

@Component
@Profile("prod")
@Slf4j
public class LambdaGeocodingClient implements GeocodingClient {

    private final LambdaClient lambdaClient;
    private final ObjectMapper objectMapper;
    private final String functionName;

    public LambdaGeocodingClient(
            @Value("${lambda.geocoding.function-name:frict-geocoding}") String functionName,
            ObjectMapper objectMapper) {
        this.lambdaClient = LambdaClient.create();
        this.objectMapper = objectMapper;
        this.functionName = functionName;
    }

    @Override
    public AddressDTO reverseGeocode(double latitude, double longitude) {
        try {
            String payload = objectMapper.writeValueAsString(
                    Map.of("op", "reverse", "lat", latitude, "lon", longitude));

            InvokeResponse response = lambdaClient.invoke(InvokeRequest.builder()
                    .functionName(functionName)
                    .payload(SdkBytes.fromUtf8String(payload))
                    .build());

            JsonNode result = objectMapper.readTree(response.payload().asByteArray());

            if (result.has("errorMessage")) {
                log.error("Lambda geocoding error: {}", result.get("errorMessage").asText());
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Geocoding provider error");
            }

            if (result.path("address").isNull() || result.path("address").isMissingNode()) {
                return null;
            }

            JsonNode addr = result.get("address");
            AddressDTO dto = new AddressDTO();
            dto.setStreet(addr.path("street").asText(""));
            dto.setNumber(addr.path("number").asText(""));
            dto.setCity(addr.path("city").asText(""));
            dto.setPostalCode(addr.path("postalCode").asText(""));
            dto.setCountry(addr.path("country").asText(""));
            dto.setLatitude(latitude);
            dto.setLongitude(longitude);
            return dto;

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lambda reverse geocoding failed", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Geocoding provider unreachable");
        }
    }

    @Override
    public CoordinatesDTO directGeocode(AddressDTO address) {
        try {
            String query = buildQuery(address);
            if (query.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Address query is empty");
            }

            String payload = objectMapper.writeValueAsString(
                    Map.of("op", "search", "address", query));

            InvokeResponse response = lambdaClient.invoke(InvokeRequest.builder()
                    .functionName(functionName)
                    .payload(SdkBytes.fromUtf8String(payload))
                    .build());

            JsonNode result = objectMapper.readTree(response.payload().asByteArray());

            if (result.has("errorMessage")) {
                log.error("Lambda geocoding error: {}", result.get("errorMessage").asText());
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Geocoding provider error");
            }

            if (result.path("lat").isMissingNode()) {
                return null;
            }

            return new CoordinatesDTO(result.get("lat").asDouble(), result.get("lon").asDouble());

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lambda direct geocoding failed", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Geocoding provider unreachable");
        }
    }

    private String buildQuery(AddressDTO address) {
        if (address == null) return "";
        StringBuilder sb = new StringBuilder();
        appendIfNotBlank(sb, address.getStreet());
        appendIfNotBlank(sb, address.getNumber());
        appendIfNotBlank(sb, address.getCity());
        appendIfNotBlank(sb, address.getPostalCode());
        appendIfNotBlank(sb, address.getCountry());
        return sb.toString().trim();
    }

    private void appendIfNotBlank(StringBuilder sb, String value) {
        if (value != null && !value.isBlank()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(value);
        }
    }
}
