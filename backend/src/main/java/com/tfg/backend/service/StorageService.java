package com.tfg.backend.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class StorageService {

    private final S3Client s3Client;

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.bucket-name}")
    private String bucketName; // "images"

    /**
     * Runs at application start.
     * 1. Empties the bucket 'images' in order to synchronize it with the empty DB (create-drop schema)
     * 2. Establishes the public reading policy for the entire bucket
     */
    @PostConstruct
    public void init() {
        try {
            // Cleaning from last execution
            ListObjectsV2Response listRes = s3Client.listObjectsV2(b -> b.bucket(bucketName));
            if (listRes.hasContents()) {
                List<ObjectIdentifier> objects = listRes.contents().stream()
                        .map(o -> ObjectIdentifier.builder().key(o.key()).build())
                        .toList();
                s3Client.deleteObjects(b -> b.bucket(bucketName).delete(d -> d.objects(objects)));
                log.info("Bucket '{}' emptied correcty.", bucketName);
            }

            // Set bucket to have public access
            String policy = """
            {
              "Version": "2012-10-17",
              "Statement": [
                {
                  "Effect": "Allow",
                  "Principal": "*",
                  "Action": ["s3:GetObject"],
                  "Resource": ["arn:aws:s3:::%s/*"]
                }
              ]
            }
            """.formatted(bucketName);

            s3Client.putBucketPolicy(b -> b.bucket(bucketName).policy(policy));
            log.info("Public policy applied to '{}'.", bucketName);

        } catch (Exception e) {
            log.error("Warning initializing MinIO: {}", e.getMessage());
        }
    }

    // Rest controllers method
    public Map<String, String> uploadFile(MultipartFile file, String folderName) throws IOException {
        return uploadFile(
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                folderName
        );
    }

    // DatabaseInitializer and internal usage method
    public Map<String, String> uploadFile(InputStream inputStream, String fileName, String contentType, long size, String folderName) throws IOException {
        String uniqueFileName = UUID.randomUUID() + "_" + fileName;
        String key = folderName + "/" + uniqueFileName;

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromInputStream(inputStream, size)
        );

        String url = String.format("%s/%s/%s", minioUrl, bucketName, key);
        return Map.of("key", key, "url", url);
    }

    public void deleteFile(String key) {
        if (key != null && !key.isBlank()) {
            s3Client.deleteObject(b -> b.bucket(bucketName).key(key));
        }
    }
}