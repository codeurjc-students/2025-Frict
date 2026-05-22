package com.tfg.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RouteDTO {
    private double durationSeconds;
    private double distanceMeters;
    private List<List<Double>> coordinates; // [[lng, lat], ...]

    public RouteDTO(double durationSeconds, double distanceMeters, List<List<Double>> coordinates) {
        this.durationSeconds = durationSeconds;
        this.distanceMeters = distanceMeters;
        this.coordinates = coordinates;
    }
}
