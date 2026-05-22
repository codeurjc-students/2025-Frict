package com.tfg.backend.controller;

import com.tfg.backend.dto.AddressDTO;
import com.tfg.backend.dto.CoordinatesDTO;
import com.tfg.backend.dto.RouteDTO;
import com.tfg.backend.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/locations")
@Tag(name = "Locations", description = "Geocoding (forward and reverse) via Nominatim proxy")
@RequiredArgsConstructor
public class LocationRestController {

    private final LocationService locationService;

    @Operation(summary = "(Admin, Manager) Reverse geocoding: get address from coordinates")
    @GetMapping("/reverse")
    public ResponseEntity<AddressDTO> reverseGeocode(@RequestParam("lat") double latitude,
                                                     @RequestParam("lng") double longitude) {
        AddressDTO result = locationService.reverseGeocode(latitude, longitude);
        return result != null ? ResponseEntity.ok(result) : ResponseEntity.noContent().build();
    }

    @Operation(summary = "(Admin, Manager) Direct geocoding: get coordinates from address")
    @PostMapping("/direct")
    public ResponseEntity<CoordinatesDTO> directGeocode(@RequestBody AddressDTO address) {
        CoordinatesDTO result = locationService.directGeocode(address);
        return result != null ? ResponseEntity.ok(result) : ResponseEntity.noContent().build();
    }

    @Operation(summary = "(All) Get driving route between two points via OSRM")
    @GetMapping("/route")
    public ResponseEntity<RouteDTO> getRoute(@RequestParam double fromLat,
                                             @RequestParam double fromLng,
                                             @RequestParam double toLat,
                                             @RequestParam double toLng) {
        RouteDTO result = locationService.getRoute(fromLat, fromLng, toLat, toLng);
        return result != null ? ResponseEntity.ok(result) : ResponseEntity.noContent().build();
    }
}
