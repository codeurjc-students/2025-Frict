package com.tfg.backend.security.oauth2;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.util.Map;

@Service
@Profile("prod")
@Slf4j
public class LambdaGoogleAuthVerifier implements GoogleAuthVerifier {

    private final LambdaClient lambdaClient;
    private final ObjectMapper objectMapper;
    private final String functionName;
    private final String clientId;

    public LambdaGoogleAuthVerifier(
            @Value("${lambda.google-auth.function-name}") String functionName,
            @Value("${google.auth.clientId}") String clientId,
            ObjectMapper objectMapper) {
        this.functionName = functionName;
        this.clientId = clientId;
        this.objectMapper = objectMapper;
        this.lambdaClient = LambdaClient.create();
    }

    @Override
    public GoogleAuthPayload verify(String idToken) {
        try {
            String payload = objectMapper.writeValueAsString(
                    Map.of("token", idToken, "clientId", clientId));

            InvokeResponse response = lambdaClient.invoke(InvokeRequest.builder()
                    .functionName(functionName)
                    .payload(SdkBytes.fromUtf8String(payload))
                    .build());

            Map<String, Object> result = objectMapper.readValue(
                    response.payload().asUtf8String(), new TypeReference<>() {});

            String errorMessage = (String) result.get("errorMessage");
            if (errorMessage != null) {
                throw new BadCredentialsException("Google auth error: " + errorMessage);
            }

            return new GoogleAuthPayload(
                    (String) result.get("email"),
                    (String) result.getOrDefault("name", ""));

        } catch (BadCredentialsException e) {
            throw e;
        } catch (Exception e) {
            throw new BadCredentialsException("Error invoking Google auth Lambda.", e);
        }
    }
}
