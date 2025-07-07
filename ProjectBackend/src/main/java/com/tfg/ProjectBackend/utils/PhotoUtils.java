package com.tfg.ProjectBackend.utils;

import com.tfg.ProjectBackend.model.Product;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import javax.sql.rowset.serial.SerialBlob;
import java.io.IOException;
import java.sql.SQLException;

public class PhotoUtils {

    public static SerialBlob setDefaultPhoto(Class<?> entityClass) {
        try {
            String imagePath = switch (entityClass.getName()) {
                case "User" -> "static/img/defaultProfilePhoto.jpg";
                case "Product" -> "static/img/defaultProductPhoto.jpg";
                default -> throw new IllegalArgumentException("No se ha definido una foto por defecto para la clase: " + entityClass.getName());
            };
            ClassPathResource imgFile = new ClassPathResource("static/img/defaultProfilePhoto.jpg");
            byte[] photoBytes = StreamUtils.copyToByteArray(imgFile.getInputStream());
            return new SerialBlob(photoBytes);
        } catch (IOException | SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
