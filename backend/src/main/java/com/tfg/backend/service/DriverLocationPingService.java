package com.tfg.backend.service;

import com.tfg.backend.dto.AddressDTO;
import com.tfg.backend.model.AddressSnapshot;
import com.tfg.backend.model.DriverLocation;
import com.tfg.backend.model.User;
import com.tfg.backend.repository.DriverLocationRepository;
import com.tfg.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverLocationPingService {

    private static final String DRIVER_ROLE = "DRIVER";
    private static final int COORD_COMPARE_DECIMALS = 5;

    private final UserRepository userRepository;
    private final LocationService locationService;
    private final DriverLocationRepository driverLocationRepository;

    public void recordPing(String username, double latitude, double longitude) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty() || !userOpt.get().getRoles().contains(DRIVER_ROLE)) {
            log.debug("Discarding GPS ping for non-driver or unknown user: {}", username);
            return;
        }
        User user = userOpt.get();

        Optional<DriverLocation> existing = driverLocationRepository.findById(username);
        AddressSnapshot snapshot = resolveSnapshot(existing, latitude, longitude);

        DriverLocation location = new DriverLocation(
                user.getUsername(),
                user.getName(),
                new Date(),
                snapshot
        );
        driverLocationRepository.save(location); // upsert by @Id (driverUsername)
        log.debug("Recorded GPS ping for driver {} at ({}, {})", username, latitude, longitude);
    }

    private AddressSnapshot resolveSnapshot(Optional<DriverLocation> existing, double latitude, double longitude) {
        if (existing.isPresent() && coordinatesMatch(existing.get(), latitude, longitude)) {
            AddressSnapshot prev = existing.get().getAddress();
            log.debug("GPS ping cache hit — reusing previous address for driver {}",
                    existing.get().getDriverUsername());
            // Refresh lat/lng on the reused snapshot to preserve the exact incoming coords
            return new AddressSnapshot(
                    prev.getStreet(), prev.getNumber(), prev.getPostalCode(),
                    prev.getCity(), prev.getCountry(),
                    latitude, longitude
            );
        }

        AddressDTO geocoded = locationService.reverseGeocode(latitude, longitude);
        if (geocoded == null) {
            return new AddressSnapshot("", "", "", "", "", latitude, longitude);
        }
        return AddressSnapshot.fromAddressDTO(geocoded, latitude, longitude);
    }

    private static boolean coordinatesMatch(DriverLocation previous, double latitude, double longitude) {
        AddressSnapshot prev = previous.getAddress();
        return round(prev.getLatitude()) == round(latitude)
                && round(prev.getLongitude()) == round(longitude);
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(COORD_COMPARE_DECIMALS, RoundingMode.HALF_UP)
                .doubleValue();
    }
}