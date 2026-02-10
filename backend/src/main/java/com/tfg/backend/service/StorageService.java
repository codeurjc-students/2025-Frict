package com.tfg.backend.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

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
    private String bucketName;

    @PostConstruct
    public void init() {
        try {
            try {
                s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            } catch (NoSuchBucketException e) {
                log.info("Bucket '{}' not found. Creating bucket...", bucketName);
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
            }

            ListObjectsV2Response listRes = s3Client.listObjectsV2(b -> b.bucket(bucketName));
            if (listRes.hasContents()) {
                List<ObjectIdentifier> objects = listRes.contents().stream()
                        .map(o -> ObjectIdentifier.builder().key(o.key()).build())
                        .toList();
                s3Client.deleteObjects(b -> b.bucket(bucketName).delete(d -> d.objects(objects)));
                log.info("Bucket '{}' emptied successfully.", bucketName);
            }

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
            log.error("CRITICAL ERROR initializing storage: {}", e.getMessage());
        }
    }

    // Accept byte[] to avoid Mark/Reset errors
    public Map<String, String> uploadFile(byte[] data, String fileName, String contentType, String folderName) {
        String uniqueFileName = UUID.randomUUID() + "_" + fileName;
        String key = folderName + "/" + uniqueFileName;

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(data) // The SDK handles the bytes array with no issues
        );

        String url = String.format("%s/%s/%s", minioUrl, bucketName, key);
        return Map.of("key", key, "url", url);
    }

    public Map<String, String> uploadFile(MultipartFile file, String folderName) throws IOException {
        return uploadFile(file.getBytes(), file.getOriginalFilename(), file.getContentType(), folderName);
    }

    public void deleteFile(String key) {
        if (key != null && !key.isBlank()) {
            s3Client.deleteObject(b -> b.bucket(bucketName).key(key));
        }
    }
}