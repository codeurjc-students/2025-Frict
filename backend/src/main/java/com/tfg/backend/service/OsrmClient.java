package com.tfg.backend.service;

import com.tfg.backend.dto.RouteDTO;

public interface OsrmClient {
    RouteDTO getRoute(double fromLat, double fromLng, double toLat, double toLng);
}
