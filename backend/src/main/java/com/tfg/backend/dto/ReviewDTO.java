package com.tfg.backend.dto;

import com.tfg.backend.model.Review;
import com.tfg.backend.notification.ConnectionDTO;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;

@Getter
@Setter
public class ReviewDTO {

    private Long id; // Review id
    private Long productId;
    private Long creatorId;
    private String productName;

    // User flattened fields
    private String creatorUsername; // Added to map the connection in Mongo
    private String creatorName;
    private String creatorImage;
    private ConnectionDTO creatorConnection; // Added to store presence data

    private String text;
    private int rating;
    private String createdAt;
    private boolean recommended;

    public ReviewDTO() {
    }

    public ReviewDTO(Review r){
        this.id = r.getId();
        this.productId = r.getProduct().getId();
        this.creatorId = r.getUser().getId();
        this.productName = r.getProduct().getName();

        this.creatorUsername = r.getUser().getUsername();
        this.creatorName = r.getUser().getName();
        // Null check for image just in case
        if (r.getUser().getUserImage() != null) {
            this.creatorImage = r.getUser().getUserImage().getImageUrl();
        }

        this.text = r.getText();
        this.rating = r.getRating();
        this.createdAt = this.formatRelativeTime(r.getCreatedAt());
        this.recommended = r.isRecommended();
        // creatorConnection remains null until enriched by the controller
    }

    private String formatRelativeTime(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(dateTime, now);

        if (duration.toMinutes() < 1) {
            return "Ahora";
        } else if (duration.toMinutes() < 60) {
            long minutes = duration.toMinutes();
            return "Hace " + minutes + " minuto" + (minutes > 1 ? "s" : "");
        } else if (duration.toHours() < 24) {
            long hours = duration.toHours();
            return "Hace " + hours + " hora" + (hours > 1 ? "s" : "");
        } else if (duration.toDays() < 30) {
            long days = duration.toDays();
            return "Hace " + days + " día" + (days > 1 ? "s" : "");
        } else if (duration.toDays() < 365) {
            long months = duration.toDays() / 30;
            return "Hace " + months + " mes" + (months > 1 ? "es" : "");
        } else {
            long years = duration.toDays() / 365;
            return "Hace " + years + " año" + (years > 1 ? "s" : "");
        }
    }
}