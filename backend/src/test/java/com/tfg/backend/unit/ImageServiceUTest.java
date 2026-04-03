package com.tfg.backend.unit;

import com.tfg.backend.model.ImageInfo;
import com.tfg.backend.service.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageServiceUTest {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private ImageService imageService;

    @Captor
    private ArgumentCaptor<PutObjectRequest> putObjectRequestCaptor;

    private final String BUCKET_NAME = "test-bucket";
    private final String PUBLIC_URL = "http://localhost:9000";

    @BeforeEach
    void setUp() {
        // Inject @Value properties manually for pure unit testing
        ReflectionTestUtils.setField(imageService, "bucketName", BUCKET_NAME);
        ReflectionTestUtils.setField(imageService, "publicUrl", PUBLIC_URL);
        ReflectionTestUtils.setField(imageService, "minioUrl", "http://minio:9000");
    }

    // --- INIT METHOD TESTS ---
    @Nested
    @DisplayName("Tests for init() @PostConstruct method")
    class InitTests {

        @Test
        @DisplayName("Creates bucket if it does not exist and applies policy")
        void init_CreatesBucket_WhenNotFound() {
            // Simulate bucket not existing
            when(s3Client.headBucket(any(HeadBucketRequest.class)))
                    .thenThrow(NoSuchBucketException.builder().build());

            // Simulate empty bucket for the listing step
            ListObjectsV2Response emptyResponse = ListObjectsV2Response.builder().isTruncated(false).build();
            when(s3Client.listObjectsV2(any(Consumer.class))).thenReturn(emptyResponse);

            imageService.init();

            // Verify bucket creation was triggered
            verify(s3Client).createBucket(any(CreateBucketRequest.class));
            // Verify policy was applied
            verify(s3Client).putBucketPolicy(any(Consumer.class));
            // Verify delete was NEVER called since bucket was empty
            verify(s3Client, never()).deleteObjects(any(Consumer.class));
        }

        @Test
        @DisplayName("Empties existing bucket if it has contents on startup")
        void init_EmptiesBucket_WhenItHasContents() {
            // Simulate bucket exists (does not throw exception)
            when(s3Client.headBucket(any(HeadBucketRequest.class))).thenReturn(HeadBucketResponse.builder().build());

            // Simulate bucket has items
            ListObjectsV2Response populatedResponse = ListObjectsV2Response.builder()
                    .contents(S3Object.builder().key("old-image.jpg").build())
                    .build();
            when(s3Client.listObjectsV2(any(Consumer.class))).thenReturn(populatedResponse);

            imageService.init();

            verify(s3Client, never()).createBucket(any(CreateBucketRequest.class));
            verify(s3Client).deleteObjects(any(Consumer.class));
            verify(s3Client).putBucketPolicy(any(Consumer.class));
        }

        @Test
        @DisplayName("Catches generic exceptions gracefully without interrupting startup")
        void init_CatchesGenericExceptions() {
            // Force a generic runtime exception during bucket check
            when(s3Client.headBucket(any(HeadBucketRequest.class))).thenThrow(new RuntimeException("S3 is down"));

            // assertDoesNotThrow ensures the catch(Exception e) block works
            assertDoesNotThrow(() -> imageService.init());
        }
    }

    // --- UPLOAD & DELETE TESTS ---
    @Nested
    @DisplayName("Tests for uploadFile and deleteFile")
    class UploadAndDeleteTests {

        @Test
        @DisplayName("uploadFile (byte[]) constructs correct URL and calls S3 putObject")
        void uploadFile_Bytes_Success() {
            byte[] fileData = "dummy-content".getBytes();
            String fileName = "test.png";
            String contentType = "image/png";
            String folder = "categories";

            Map<String, String> result = imageService.uploadFile(fileData, fileName, contentType, folder);

            // Verify S3 client interaction
            verify(s3Client).putObject(putObjectRequestCaptor.capture(), any(RequestBody.class));
            PutObjectRequest capturedRequest = putObjectRequestCaptor.getValue();

            assertEquals(BUCKET_NAME, capturedRequest.bucket());
            assertEquals(contentType, capturedRequest.contentType());
            assertTrue(capturedRequest.key().startsWith(folder + "/"));
            assertTrue(capturedRequest.key().endsWith("_" + fileName)); // Checks UUID appending

            // Verify returned Map
            assertNotNull(result.get("key"));
            assertEquals(PUBLIC_URL + "/" + BUCKET_NAME + "/" + result.get("key"), result.get("url"));
        }

        @Test
        @DisplayName("deleteFile calls S3 only if key is valid")
        void deleteFile_ValidKey_CallsS3() {
            imageService.deleteFile("folder/image.png");
            verify(s3Client).deleteObject(any(Consumer.class));
        }

        @Test
        @DisplayName("deleteFile ignores null or blank keys")
        void deleteFile_BlankKey_DoesNothing() {
            imageService.deleteFile("");
            imageService.deleteFile(null);
            imageService.deleteFile("   ");

            verify(s3Client, never()).deleteObject(any(Consumer.class));
        }
    }

    // --- BUSINESS LOGIC: REPLACEMENT & INFO ---
    @Nested
    @DisplayName("Tests for processImageReplacement and uploadImageAndGetInfo")
    class ReplacementLogicTests {

        private MockMultipartFile validFile;

        @BeforeEach
        void setup() {
            validFile = new MockMultipartFile(
                    "file", "new-pic.jpg", "image/jpeg", "image-data".getBytes()
            );
        }

        @Test
        @DisplayName("uploadImageAndGetInfo returns null if file is empty")
        void uploadImageAndGetInfo_ReturnsNull_WhenFileEmpty() {
            MultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);
            assertNull(imageService.uploadImageAndGetInfo(emptyFile, "folder"));
        }

        @Test
        @DisplayName("uploadImageAndGetInfo throws 500 INTERNAL_SERVER_ERROR on IOException")
        void uploadImageAndGetInfo_ThrowsException_OnIOError() throws IOException {
            MultipartFile corruptedFile = mock(MultipartFile.class);
            when(corruptedFile.isEmpty()).thenReturn(false);
            when(corruptedFile.getBytes()).thenThrow(new IOException("Disk error"));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> imageService.uploadImageAndGetInfo(corruptedFile, "folder"));

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
            assertEquals("Could not upload image to S3", ex.getReason());
        }

        @Test
        @DisplayName("processImageReplacement logic: Replaces old custom image with new one")
        void processImageReplacement_ReplacesCustomImage() {
            // Arrange
            ImageInfo oldCustomImage = new ImageInfo("url", "old-key.jpg", "old.jpg");
            Predicate<ImageInfo> isDefault = img -> false; // Simulate it's NOT a default image
            Supplier<ImageInfo> defaultSupplier = ImageInfo::new;

            // Act
            ImageInfo result = imageService.processImageReplacement(oldCustomImage, validFile, "folder", isDefault, defaultSupplier);

            // Assert
            verify(s3Client).deleteObject(any(Consumer.class)); // Verifies old image was deleted
            verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class)); // Verifies new upload
            assertNotNull(result);
            assertEquals("new-pic.jpg", result.getFileName());
        }

        @Test
        @DisplayName("processImageReplacement logic: Keeps old default image, uploads new one")
        void processImageReplacement_KeepsDefaultImage_UploadsNew() {
            // Arrange
            ImageInfo defaultImage = new ImageInfo("default-url", "default-key", "default.jpg");
            Predicate<ImageInfo> isDefault = img -> true; // Simulate it IS a default image
            Supplier<ImageInfo> defaultSupplier = ImageInfo::new;

            // Act
            imageService.processImageReplacement(defaultImage, validFile, "folder", isDefault, defaultSupplier);

            // Assert
            verify(s3Client, never()).deleteObject(any(Consumer.class)); // MUST NOT delete default image
            verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class)); // Verifies new upload
        }

        @Test
        @DisplayName("processImageReplacement logic: Deletes old custom image, falls back to default if no new file")
        void processImageReplacement_DeletesCustom_FallsBackToDefault() {
            // Arrange
            ImageInfo oldCustomImage = new ImageInfo("url", "old-key.jpg", "old.jpg");
            ImageInfo fallbackDefault = new ImageInfo("def-url", "def-key", "def.jpg");

            Predicate<ImageInfo> isDefault = img -> false;
            Supplier<ImageInfo> defaultSupplier = () -> fallbackDefault;

            // Act: Pass a null MultipartFile
            ImageInfo result = imageService.processImageReplacement(oldCustomImage, null, "folder", isDefault, defaultSupplier);

            // Assert
            verify(s3Client).deleteObject(any(Consumer.class)); // Verifies old was deleted
            verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class)); // No new upload
            assertEquals(fallbackDefault, result); // Ensures fallback was returned
        }
    }
}