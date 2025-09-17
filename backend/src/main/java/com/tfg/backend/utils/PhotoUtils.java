package com.tfg.backend.utils;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import javax.sql.rowset.serial.SerialBlob;
import java.io.IOException;
import java.sql.SQLException;

public class PhotoUtils {

    public static SerialBlob setDefaultPhoto(Class<?> entityClass) {
        try {
            String imagePath = switch (entityClass.getSimpleName()) {
                case "User" -> "static/img/defaultProfilePhoto.jpg";
                case "Product" -> "static/img/defaultProductPhoto.jpg";
                default -> throw new IllegalArgumentException("No se ha definido una foto por defecto para la clase: " + entityClass.getSimpleName());
            };
            ClassPathResource imgFile = new ClassPathResource(imagePath);
            byte[] photoBytes = StreamUtils.copyToByteArray(imgFile.getInputStream());
            return new SerialBlob(photoBytes);
        } catch (IOException | SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
