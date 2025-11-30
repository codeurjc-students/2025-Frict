package com.tfg.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String alias;

    private String street;

    private String number;

    private String floor;

    private String postalCode;

    private String city;

    private String country;

    public Address() {
    }

    public Address(String alias, String street, String number, String floor, String postalCode, String city, String country) {
        this.alias = alias;
        this.street = street;
        this.number = number;
        this.floor = floor;
        this.postalCode = postalCode;
        this.city = city;
        this.country = country;
    }

    @Override
    public String toString() {
        return street + " " + number + ", " + floor + " " + postalCode + " " + city + " (" + country + ")";
    }
}
