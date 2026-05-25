package com.tfg.backend.service;

import com.tfg.backend.model.ImageInfo;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageService {

    private final S3Client s3Client;

    @Value("${app.storage.public-url}")
    private String publicUrl;

    @Value("${app.storage.bucket-name}")
    private String bucketName;

    @PostConstruct
    public void init() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            log.info("Bucket '{}' is accessible.", bucketName);
        } catch (NoSuchBucketException e) {
            log.info("Bucket '{}' not found, creating...", bucketName);
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        } catch (Exception e) {
            log.error("Could not verify bucket '{}': {}", bucketName, e.getMessage());
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
                RequestBody.fromBytes(data)
        );

        // Use publicUrl instead of minioUrl
        String url = String.format("%s/%s/%s", publicUrl, bucketName, key);
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


    //Entities image replacement
    public ImageInfo uploadImageAndGetInfo(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) return null;
        try {
            Map<String, String> res = this.uploadFile(file, folder);
            return new ImageInfo(res.get("url"), res.get("key"), file.getOriginalFilename());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not upload image to S3");
        }
    }


    public ImageInfo processImageReplacement(
            ImageInfo currentImage,
            MultipartFile newFile,
            String folder,
            Predicate<ImageInfo> isDefaultChecker,
            Supplier<ImageInfo> defaultImageSupplier) {

        // 1. Delete the old one if is not the default image
        if (currentImage != null && !isDefaultChecker.test(currentImage)) {
            this.deleteFile(currentImage.getS3Key());
        }

        // 2. Upload new or set default
        if (newFile != null && !newFile.isEmpty()) {
            return this.uploadImageAndGetInfo(newFile, folder);
        } else {
            return defaultImageSupplier.get();
        }
    }
}