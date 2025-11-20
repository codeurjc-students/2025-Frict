package com.tfg.backend.DTO;

import com.tfg.backend.model.Review;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;

@Getter
@Setter
public class ReviewDTO {

    private Long id; //Review id
    private Long creatorId;
    private String creatorName;
    private String creatorThumbnailUrl;
    private String text;
    private int rating;
    private String createdAt;
    private boolean recommended;

    public ReviewDTO() {
    }

    public ReviewDTO(Review r){
        this.id = r.getId();
        this.creatorId = r.getUser().getId();
        this.creatorName = r.getUser().getName();
        this.creatorThumbnailUrl = "/api/v1/users/thumbnail/" + r.getUser().getId();
        this.text = r.getText();
        this.rating = r.getRating();
        this.createdAt = this.formatRelativeTime(r.getCreatedAt());
        this.recommended = r.isRecommended();
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
