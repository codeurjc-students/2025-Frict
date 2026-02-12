package com.tfg.backend.unit;

import com.tfg.backend.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageServiceUTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private StorageService storageService;

    private final String BUCKET_NAME = "test-images";
    private final String PUBLIC_URL = "http://localhost:9001";
    private final String MINIO_URL = "http://minio:9000";

    @BeforeEach
    void setUp() {
        // Injecting @Value fields manually for the unit test
        ReflectionTestUtils.setField(storageService, "bucketName", BUCKET_NAME);
        ReflectionTestUtils.setField(storageService, "publicUrl", PUBLIC_URL);
        ReflectionTestUtils.setField(storageService, "minioUrl", MINIO_URL);
    }

    // --- INIT TESTS ---

    @Test
    void init_ShouldCreateBucket_IfItDoesNotExist() {
        when(s3Client.headBucket(any(HeadBucketRequest.class)))
                .thenThrow(NoSuchBucketException.builder().build());

        ListObjectsV2Response emptyResponse = ListObjectsV2Response.builder().build();
        when(s3Client.listObjectsV2(any(Consumer.class))).thenReturn(emptyResponse);

        storageService.init();

        verify(s3Client).createBucket(any(CreateBucketRequest.class));
        verify(s3Client).putBucketPolicy(any(Consumer.class));
    }

    @Test
    void init_ShouldEmptyBucket_IfItHasContent() {
        when(s3Client.headBucket(any(HeadBucketRequest.class))).thenReturn(HeadBucketResponse.builder().build());

        S3Object s3Object = S3Object.builder().key("old.jpg").build();
        ListObjectsV2Response listResponse = ListObjectsV2Response.builder()
                .contents(s3Object)
                .isTruncated(false)
                .build();

        when(s3Client.listObjectsV2(any(Consumer.class))).thenReturn(listResponse);

        storageService.init();

        verify(s3Client).deleteObjects(any(Consumer.class));
        verify(s3Client).putBucketPolicy(any(Consumer.class));
    }

    // --- UPLOAD TESTS ---

    @Test
    void uploadFile_Multipart_ShouldUploadAndReturnUrl() throws IOException {
        String fileName = "test.jpg";
        String contentType = "image/jpeg";
        String folderName = "products";
        byte[] content = "fake-image-content".getBytes();

        // Stubbing getBytes() is required because the service calls file.getBytes()
        when(multipartFile.getBytes()).thenReturn(content);
        when(multipartFile.getOriginalFilename()).thenReturn(fileName);
        when(multipartFile.getContentType()).thenReturn(contentType);

        Map<String, String> result = storageService.uploadFile(multipartFile, folderName);

        assertNotNull(result);
        String resultKey = result.get("key");
        String resultUrl = result.get("url");

        assertTrue(resultKey.startsWith(folderName + "/"));
        assertTrue(resultKey.endsWith("_" + fileName));

        // URL must be constructed using publicUrl, not internal minioUrl
        String expectedUrlStart = PUBLIC_URL + "/" + BUCKET_NAME + "/" + folderName;
        assertTrue(resultUrl.startsWith(expectedUrlStart));

        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void uploadFile_ByteArray_ShouldUploadAndReturnMap() {
        String fileName = "data.txt";
        String contentType = "text/plain";
        String folderName = "docs";
        byte[] content = "content".getBytes();

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);

        Map<String, String> result = storageService.uploadFile(content, fileName, contentType, folderName);

        assertNotNull(result);
        String key = result.get("key");
        String url = result.get("url");

        String expectedUrl = String.format("%s/%s/%s", PUBLIC_URL, BUCKET_NAME, key);
        assertEquals(expectedUrl, url);

        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest capturedRequest = requestCaptor.getValue();
        assertEquals(BUCKET_NAME, capturedRequest.bucket());
        assertEquals(key, capturedRequest.key());
        assertEquals(contentType, capturedRequest.contentType());
    }

    // --- DELETE TESTS ---

    @Test
    void deleteFile_ShouldDelete_WhenKeyIsValid() {
        String key = "folder/image.jpg";
        storageService.deleteFile(key);
        verify(s3Client, times(1)).deleteObject(any(Consumer.class));
    }

    @Test
    void deleteFile_ShouldDoNothing_WhenKeyIsNull() {
        storageService.deleteFile(null);
        verify(s3Client, never()).deleteObject(any(Consumer.class));
    }
}