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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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
    private final String MINIO_URL = "http://localhost:9000";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(storageService, "bucketName", BUCKET_NAME);
        ReflectionTestUtils.setField(storageService, "minioUrl", MINIO_URL);
    }


    // init() method tests
    @Test
    void init_ShouldEmptyBucketAndSetPolicy_WhenBucketHasContent() {
        S3Object s3Object = S3Object.builder().key("old-image.jpg").build();
        ListObjectsV2Response listResponse = ListObjectsV2Response.builder()
                .contents(s3Object)
                .isTruncated(false)
                .build();

        when(s3Client.listObjectsV2(any(Consumer.class))).thenReturn(listResponse);

        storageService.init();

        verify(s3Client, times(1)).listObjectsV2(any(Consumer.class));
        verify(s3Client, times(1)).deleteObjects(any(Consumer.class));
        verify(s3Client, times(1)).putBucketPolicy(any(Consumer.class));
    }

    @Test
    void init_ShouldSetPolicy_EvenWhenBucketIsEmpty() {
        ListObjectsV2Response emptyResponse = ListObjectsV2Response.builder().build();
        when(s3Client.listObjectsV2(any(Consumer.class))).thenReturn(emptyResponse);

        storageService.init();

        verify(s3Client, never()).deleteObjects(any(Consumer.class));
        verify(s3Client, times(1)).putBucketPolicy(any(Consumer.class));
    }

    @Test
    void init_ShouldHandleExceptionGracefully() {
        when(s3Client.listObjectsV2(any(Consumer.class))).thenThrow(new RuntimeException("S3 Error"));

        assertDoesNotThrow(() -> storageService.init());

        verify(s3Client, times(1)).listObjectsV2(any(Consumer.class));
    }


    // uploadFile() method tests
    @Test
    void uploadFile_Multipart_ShouldUploadAndReturnUrl() throws IOException {
        String fileName = "test.jpg";
        String contentType = "image/jpeg";
        String folderName = "products";
        long size = 100L;
        InputStream inputStream = new ByteArrayInputStream(new byte[0]);

        when(multipartFile.getInputStream()).thenReturn(inputStream);
        when(multipartFile.getOriginalFilename()).thenReturn(fileName);
        when(multipartFile.getContentType()).thenReturn(contentType);
        when(multipartFile.getSize()).thenReturn(size);

        Map<String, String> result = storageService.uploadFile(multipartFile, folderName);

        assertNotNull(result);
        assertTrue(result.containsKey("key"));
        assertTrue(result.containsKey("url"));

        String resultKey = result.get("key");
        assertTrue(resultKey.startsWith(folderName + "/"));
        assertTrue(resultKey.endsWith("_" + fileName));

        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
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

        assertTrue(key.startsWith(folderName + "/"));
        assertTrue(key.endsWith("_" + fileName));
        assertEquals(String.format("%s/%s/%s", MINIO_URL, BUCKET_NAME, key), url);

        // S3 Client interaction verification
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest capturedRequest = requestCaptor.getValue();
        assertEquals(BUCKET_NAME, capturedRequest.bucket());
        assertEquals(key, capturedRequest.key());
        assertEquals(contentType, capturedRequest.contentType());
    }


    // deleteFile() method tests
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

    @Test
    void deleteFile_ShouldDoNothing_WhenKeyIsBlank() {
        storageService.deleteFile("   ");
        verify(s3Client, never()).deleteObject(any(Consumer.class));
    }
}
