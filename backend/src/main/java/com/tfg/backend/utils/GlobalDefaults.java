package com.tfg.backend.utils;

import com.tfg.backend.model.ImageInfo;
import com.tfg.backend.model.ProductImageInfo;

public class GlobalDefaults {

    // Tu DatabaseInitializer seguirá llenando estas variables al arrancar
    public static ImageInfo USER_IMAGE;
    public static ImageInfo CATEGORY_IMAGE;
    public static ImageInfo PRODUCT_IMAGE;
    public static ImageInfo SHOP_IMAGE;

    private GlobalDefaults() {}

    // --- 1. MÉTODOS FACTORÍA (Para ASIGNAR imágenes de forma segura para Hibernate) ---

    public static ImageInfo getDefaultUserImage() {
        if (USER_IMAGE == null) return null;
        return new ImageInfo(USER_IMAGE.getImageUrl(), USER_IMAGE.getS3Key(), USER_IMAGE.getFileName());
    }

    public static ImageInfo getDefaultCategoryImage() {
        if (CATEGORY_IMAGE == null) return null;
        return new ImageInfo(CATEGORY_IMAGE.getImageUrl(), CATEGORY_IMAGE.getS3Key(), CATEGORY_IMAGE.getFileName());
    }

    public static ImageInfo getDefaultProductImage() {
        if (PRODUCT_IMAGE == null) return null;
        return new ImageInfo(PRODUCT_IMAGE.getImageUrl(), PRODUCT_IMAGE.getS3Key(), PRODUCT_IMAGE.getFileName());
    }

    public static ImageInfo getDefaultShopImage() {
        if (SHOP_IMAGE == null) return null;
        return new ImageInfo(SHOP_IMAGE.getImageUrl(), SHOP_IMAGE.getS3Key(), SHOP_IMAGE.getFileName());
    }

    // --- 2. MÉTODOS DE COMPARACIÓN (Para COMPROBAR antes de borrar de S3) ---

    public static boolean isDefaultUserImage(ImageInfo imageToCheck) {
        return imageToCheck != null && USER_IMAGE != null &&
                imageToCheck.getS3Key().equals(USER_IMAGE.getS3Key());
    }

    public static boolean isDefaultCategoryImage(ImageInfo imageToCheck) {
        return imageToCheck != null && CATEGORY_IMAGE != null &&
                imageToCheck.getS3Key().equals(CATEGORY_IMAGE.getS3Key());
    }

    public static boolean isDefaultShopImage(ImageInfo imageToCheck) {
        return imageToCheck != null && SHOP_IMAGE != null &&
                imageToCheck.getS3Key().equals(SHOP_IMAGE.getS3Key());
    }

    // Fíjate que este recibe un ProductImageInfo, extrayendo su imageInfo interno
    public static boolean isDefaultProductImage(ProductImageInfo productImageToCheck) {
        return productImageToCheck != null &&
                productImageToCheck.getImageInfo() != null &&
                PRODUCT_IMAGE != null &&
                productImageToCheck.getImageInfo().getS3Key().equals(PRODUCT_IMAGE.getS3Key());
    }
}