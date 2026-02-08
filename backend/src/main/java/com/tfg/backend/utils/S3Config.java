package com.tfg.backend.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.security.cert.X509Certificate;

@Configuration
@Slf4j
public class S3Config {

    @Value("${minio.url}")
    private String url;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.ssl.insecure:false}")
    private boolean insecureMode;

    @Bean
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build());

        // Conditional logic: Local (Insecure MinIO) vs Prod (Secure AWS S3)
        if (insecureMode) {
            // Inject the modified UrlConnection client
            builder.httpClient(getUnsafeUrlConnectionClient());
            builder.endpointOverride(URI.create(url));
            log.warn("MinIO: Using UrlConnectionHttpClient in INSECURE mode.");
        } else {
            //In production instances, let SDK use its own default client (also UrlConnection), but with SSL security activated by default
            builder.httpClient(UrlConnectionHttpClient.builder().build());

            if (!url.isEmpty() && !url.contains("amazonaws.com")) {
                builder.endpointOverride(URI.create(url));
            }
        }

        return builder.build();
    }

    /**
     * Creates an instance of UrlConnectionHttpClient that trusts any certificate.
     */
    private SdkHttpClient getUnsafeUrlConnectionClient() {
        // Create the TrustManager, that does not make any validations
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
        };

        // Build the UrlConnectionHttpClient by injecting the created TrustManager
        return UrlConnectionHttpClient.builder()
                .tlsTrustManagersProvider(() -> trustAllCerts)
                .build();
    }
}