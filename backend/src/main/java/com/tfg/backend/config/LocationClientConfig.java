package com.tfg.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
public class LocationClientConfig {

    @Bean
    public RestClient nominatimRestClient(
            @Value("${nominatim.base-url}") String baseUrl,
            @Value("${nominatim.user-agent}") String userAgent) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
                .build();
    }

    @Bean
    public RestClient osrmRestClient(@Value("${osrm.base-url}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
