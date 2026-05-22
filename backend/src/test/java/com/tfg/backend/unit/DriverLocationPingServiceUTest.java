package com.tfg.backend.unit;

import com.tfg.backend.dto.AddressDTO;
import com.tfg.backend.model.AddressSnapshot;
import com.tfg.backend.model.DriverLocation;
import com.tfg.backend.model.User;
import com.tfg.backend.repository.DriverLocationRepository;
import com.tfg.backend.repository.UserRepository;
import com.tfg.backend.service.DriverLocationPingService;
import com.tfg.backend.service.LocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverLocationPingServiceUTest {

    @Mock private UserRepository userRepository;
    @Mock private LocationService locationService;
    @Mock private DriverLocationRepository driverLocationRepository;

    @InjectMocks private DriverLocationPingService driverLocationPingService;

    private User driverUser;

    @BeforeEach
    void setUp() {
        driverUser = new User();
        driverUser.setUsername("driver1");
        driverUser.setName("Carlos Conductor");
        Set<String> roles = new HashSet<>();
        roles.add("DRIVER");
        driverUser.setRoles(roles);
    }

    @Test
    @DisplayName("Discards ping when user has no DRIVER role")
    void recordPing_NonDriver_DiscardsSilently() {
        User nonDriver = new User();
        nonDriver.setUsername("user1");
        nonDriver.setRoles(new HashSet<>(Set.of("USER")));
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(nonDriver));

        driverLocationPingService.recordPing("user1", 40.0, -3.0);

        verifyNoInteractions(locationService);
        verify(driverLocationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Discards ping when user does not exist")
    void recordPing_UnknownUser_DiscardsSilently() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        driverLocationPingService.recordPing("ghost", 40.0, -3.0);

        verifyNoInteractions(locationService);
        verify(driverLocationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Calls reverseGeocode and saves a new DriverLocation when no document exists")
    void recordPing_FirstPing_CallsGeocode() {
        when(userRepository.findByUsername("driver1")).thenReturn(Optional.of(driverUser));
        when(driverLocationRepository.findById("driver1")).thenReturn(Optional.empty());

        AddressDTO geocoded = new AddressDTO();
        geocoded.setStreet("Gran Vía");
        geocoded.setCity("Madrid");
        geocoded.setCountry("España");
        geocoded.setLatitude(40.4168);
        geocoded.setLongitude(-3.7038);
        when(locationService.reverseGeocode(40.4168, -3.7038)).thenReturn(geocoded);

        driverLocationPingService.recordPing("driver1", 40.4168, -3.7038);

        ArgumentCaptor<DriverLocation> captor = ArgumentCaptor.forClass(DriverLocation.class);
        verify(driverLocationRepository).save(captor.capture());
        DriverLocation saved = captor.getValue();

        assertEquals("driver1", saved.getDriverUsername());
        assertEquals("Carlos Conductor", saved.getDriverName());
        assertEquals("Gran Vía", saved.getAddress().getStreet());
        assertEquals("Madrid", saved.getAddress().getCity());
        assertEquals(40.4168, saved.getAddress().getLatitude());
        assertNotNull(saved.getPingDateTime());
    }

    @Test
    @DisplayName("Reuses previous address (cache hit) when rounded coordinates match")
    void recordPing_SameCoordinates_ReusesAddress() {
        when(userRepository.findByUsername("driver1")).thenReturn(Optional.of(driverUser));

        AddressSnapshot previousAddress = new AddressSnapshot(
                "Calle Sol", "5", "28010", "Madrid", "España", 40.41680, -3.70380);
        DriverLocation existing = new DriverLocation(
                "driver1", "Carlos Conductor", new Date(0), previousAddress);

        when(driverLocationRepository.findById("driver1")).thenReturn(Optional.of(existing));

        // Same coordinates at 5 decimals (noise in the 6th decimal is ignored)
        driverLocationPingService.recordPing("driver1", 40.416801, -3.703799);

        verify(locationService, never()).reverseGeocode(anyDouble(), anyDouble());

        ArgumentCaptor<DriverLocation> captor = ArgumentCaptor.forClass(DriverLocation.class);
        verify(driverLocationRepository).save(captor.capture());
        DriverLocation saved = captor.getValue();
        assertEquals("Calle Sol", saved.getAddress().getStreet());
        assertEquals("Madrid", saved.getAddress().getCity());
        // The stored snapshot keeps the exact incoming coords, not the previous ones
        assertEquals(40.416801, saved.getAddress().getLatitude());
        assertEquals(-3.703799, saved.getAddress().getLongitude());
        // pingDateTime is refreshed (newer than the previous Date(0))
        assertTrue(saved.getPingDateTime().getTime() > 0);
    }

    @Test
    @DisplayName("Calls reverseGeocode when previous coordinates differ at 5 decimals")
    void recordPing_DifferentCoordinates_CallsGeocode() {
        when(userRepository.findByUsername("driver1")).thenReturn(Optional.of(driverUser));

        AddressSnapshot previousAddress = new AddressSnapshot(
                "Calle Sol", "5", "28010", "Madrid", "España", 40.4168, -3.7038);
        DriverLocation existing = new DriverLocation(
                "driver1", "Carlos Conductor", new Date(0), previousAddress);

        when(driverLocationRepository.findById("driver1")).thenReturn(Optional.of(existing));

        AddressDTO geocoded = new AddressDTO();
        geocoded.setStreet("Otra Calle");
        geocoded.setLatitude(40.5000);
        geocoded.setLongitude(-3.8000);
        when(locationService.reverseGeocode(40.5000, -3.8000)).thenReturn(geocoded);

        driverLocationPingService.recordPing("driver1", 40.5000, -3.8000);

        verify(locationService).reverseGeocode(40.5000, -3.8000);
        ArgumentCaptor<DriverLocation> captor = ArgumentCaptor.forClass(DriverLocation.class);
        verify(driverLocationRepository).save(captor.capture());
        assertEquals("Otra Calle", captor.getValue().getAddress().getStreet());
    }

    @Test
    @DisplayName("Stores a coords-only snapshot when reverseGeocode returns null")
    void recordPing_NominatimReturnsNull_StoresCoordsOnly() {
        when(userRepository.findByUsername("driver1")).thenReturn(Optional.of(driverUser));
        when(driverLocationRepository.findById("driver1")).thenReturn(Optional.empty());
        when(locationService.reverseGeocode(0.0, 0.0)).thenReturn(null);

        driverLocationPingService.recordPing("driver1", 0.0, 0.0);

        ArgumentCaptor<DriverLocation> captor = ArgumentCaptor.forClass(DriverLocation.class);
        verify(driverLocationRepository).save(captor.capture());
        AddressSnapshot saved = captor.getValue().getAddress();
        assertEquals("", saved.getStreet());
        assertEquals("", saved.getCity());
        assertEquals(0.0, saved.getLatitude());
        assertEquals(0.0, saved.getLongitude());
    }
}