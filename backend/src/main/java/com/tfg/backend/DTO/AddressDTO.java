package com.tfg.backend.DTO;

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

    public AddressDTO() {
    }

    public AddressDTO(String alias, String street, String number, String floor, String postalCode, String city, String country) {
        this.alias = alias;
        this.street = street;
        this.number = number;
        this.floor = floor;
        this.postalCode = postalCode;
        this.city = city;
        this.country = country;
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
    }
}
