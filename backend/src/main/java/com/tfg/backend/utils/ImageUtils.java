package com.tfg.backend.utils;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.rowset.serial.SerialBlob;
import java.io.IOException;
import java.sql.Blob;
import java.sql.SQLException;

public class ImageUtils {

    public static ResponseEntity<byte[]> serveImage(Blob imageBlob){
        if (imageBlob == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            byte[] imageBytes = imageBlob.getBinaryStream().readAllBytes();
            return ResponseEntity
                    .ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(imageBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    public static Blob prepareImage(MultipartFile image) {
        try {
            return new SerialBlob(image.getBytes());
        }
        catch (IOException | SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public static Blob prepareDefaultImage(Class<?> entityClass) {
        try {
            String imagePath = switch (entityClass.getSimpleName()) {
                case "User" -> "static/img/defaultProfileImage.jpg";
                case "Product" -> "static/img/defaultProductImage.jpg";
                case "Category" -> "static/img/defaultCategoryImage.jpg";
                default -> throw new IllegalArgumentException("No se ha definido una foto por defecto para la clase: " + entityClass.getSimpleName());
            };
            ClassPathResource imgFile = new ClassPathResource(imagePath);
            byte[] imageBytes = StreamUtils.copyToByteArray(imgFile.getInputStream());
            return new SerialBlob(imageBytes);
        } catch (IOException | SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
