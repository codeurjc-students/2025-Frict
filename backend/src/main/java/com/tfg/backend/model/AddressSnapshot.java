package com.tfg.backend.model;

import com.tfg.backend.dto.AddressDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddressSnapshot {

    private String street;
    private String number;
    private String postalCode;
    private String city;
    private String country;
    private double latitude;
    private double longitude;

    public static AddressSnapshot fromAddressDTO(AddressDTO dto, double fallbackLat, double fallbackLng) {
        if (dto == null) {
            return new AddressSnapshot("", "", "", "", "", fallbackLat, fallbackLng);
        }
        return new AddressSnapshot(
                dto.getStreet() == null ? "" : dto.getStreet(),
                dto.getNumber() == null ? "" : dto.getNumber(),
                dto.getPostalCode() == null ? "" : dto.getPostalCode(),
                dto.getCity() == null ? "" : dto.getCity(),
                dto.getCountry() == null ? "" : dto.getCountry(),
                dto.getLatitude(),
                dto.getLongitude()
        );
    }
}
