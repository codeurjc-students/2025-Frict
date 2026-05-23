package com.tfg.backend.service;

import com.tfg.backend.dto.AddressDTO;
import com.tfg.backend.dto.CoordinatesDTO;

public interface GeocodingClient {

    AddressDTO reverseGeocode(double latitude, double longitude);

    CoordinatesDTO directGeocode(AddressDTO address);
}
