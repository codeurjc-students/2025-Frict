package com.tfg.backend.dto;

import com.tfg.backend.model.Address;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressDTO {
    private Long id;
    private String alias;
    private String street;
    private String number;
    private String floor;
    private String postalCode;
    private String city;
    private String country;

    private double latitude;
    private double longitude;

    public AddressDTO() {
    }

    public AddressDTO(Address address) {
        this.id = address.getId();
        this.alias = address.getAlias();
        this.street = address.getStreet();
        this.number = address.getNumber();
        this.floor = address.getFloor();
        this.postalCode = address.getPostalCode();
        this.city = address.getCity();
        this.country = address.getCountry();
        this.latitude = address.getLatitude();
        this.longitude = address.getLongitude();
    }

    @Override
    public String toString(){
        return this.street + ", " + this.number + " " + this.postalCode + " " + this.city + " (" + this.country + ")";
    }
}
