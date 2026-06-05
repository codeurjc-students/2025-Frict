package com.tfg.backend.service;

import com.tfg.backend.dto.AddressDTO;
import com.tfg.backend.dto.CoordinatesDTO;
import com.tfg.backend.dto.RouteDTO;
import org.springframework.stereotype.Service;

@Service
public class LocationService {

    private final GeocodingClient geocodingClient;
    private final OsrmClient osrmClient;

    public LocationService(GeocodingClient geocodingClient, OsrmClient osrmClient) {
        this.geocodingClient = geocodingClient;
        this.osrmClient = osrmClient;
    }

    public AddressDTO reverseGeocode(double latitude, double longitude) {
        return geocodingClient.reverseGeocode(latitude, longitude);
    }

    public CoordinatesDTO directGeocode(AddressDTO address) {
        return geocodingClient.directGeocode(address);
    }

    public RouteDTO getRoute(double fromLat, double fromLng, double toLat, double toLng) {
        return osrmClient.getRoute(fromLat, fromLng, toLat, toLng);
    }
}
