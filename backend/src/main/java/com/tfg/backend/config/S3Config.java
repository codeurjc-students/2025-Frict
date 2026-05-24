package com.tfg.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;

@Configuration
public class S3Config {

    private static final Logger log = LoggerFactory.getLogger(S3Config.class);

    @Value("${app.storage.region:us-east-1}")
    private String region;

    @Value("${app.storage.endpoint:#{null}}")
    private String endpoint;

    @Value("${AWS_ACCESS_KEY_ID:#{null}}")
    private String accessKey;

    @Value("${AWS_SECRET_ACCESS_KEY:#{null}}")
    private String secretKey;

    @Bean
    public S3Client s3Client() {
        log.debug("S3Config — endpoint={}, accessKey={}", endpoint, accessKey != null ? "***" : "null");
        if (endpoint != null && endpoint.startsWith("https://")) {
            trustLocalCert();
        }

        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build());

        if (accessKey != null && secretKey != null) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        if (endpoint != null && !endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        return builder.build();
    }

    private void trustLocalCert() {
        try {
            ClassPathResource certResource = new ClassPathResource("certs/localhost.crt");
            if (!certResource.exists()) return;

            Path cacertsPath = Path.of(System.getProperty("java.home"), "lib", "security", "cacerts");
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            try (InputStream is = Files.newInputStream(cacertsPath)) {
                ks.load(is, "changeit".toCharArray());
            }

            try (InputStream certStream = certResource.getInputStream()) {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                ks.setCertificateEntry("minio-local", cf.generateCertificate(certStream));
            }

            Path truststore = Files.createTempFile("frict-truststore-", ".jks");
            truststore.toFile().deleteOnExit();
            try (OutputStream os = Files.newOutputStream(truststore)) {
                ks.store(os, "changeit".toCharArray());
            }
            System.setProperty("javax.net.ssl.trustStore", truststore.toAbsolutePath().toString());
            System.setProperty("javax.net.ssl.trustStorePassword", "changeit");

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, tmf.getTrustManagers(), null);
            SSLContext.setDefault(ctx);
            HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());

            log.info("Imported classpath:certs/localhost.crt into JVM truststore at {}", truststore);
        } catch (Exception e) {
            log.warn("Could not load local certificate for S3: {}", e.getMessage());
        }
    }
}
