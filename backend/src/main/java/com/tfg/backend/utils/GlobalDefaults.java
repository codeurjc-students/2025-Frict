package com.tfg.backend.utils;

import com.tfg.backend.model.ImageInfo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GlobalDefaults {
    public static ImageInfo USER_IMAGE;
    public static ImageInfo CATEGORY_IMAGE;
    public static ImageInfo PRODUCT_IMAGE;
    public static ImageInfo SHOP_IMAGE;

    private GlobalDefaults() {}
}
