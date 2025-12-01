package com.tfg.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class ImageInfo {

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "s3_key")
    private String s3Key; // Necessary in order to delete from MinIO

    private String fileName; // Optional

    public ImageInfo() {
    }

    public ImageInfo(String imageUrl, String s3Key, String fileName) {
        this.imageUrl = imageUrl;
        this.s3Key = s3Key;
        this.fileName = fileName;
    }
}
