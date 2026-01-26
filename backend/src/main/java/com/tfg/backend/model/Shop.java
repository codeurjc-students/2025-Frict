package com.tfg.backend.model;

import com.tfg.backend.utils.ReferenceNumberGenerator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "shops")
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String referenceCode;

    private String name;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "address_id")
    private Address address;

    @Embedded
    private ImageInfo image;

    private double longitude;

    private double latitude;

    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ShopStock> availableProducts = new HashSet<>();

    @OneToMany(mappedBy = "assignedShop")
    private Set<Truck> assignedTrucks = new HashSet<>();

    public Shop() {
    }

    public Shop(String name, Address address, double longitude, double latitude) {
        this.referenceCode = ReferenceNumberGenerator.generateShopReferenceNumber();
        this.name = name;
        this.address = address;
        this.longitude = longitude;
        this.latitude = latitude;
    }
}
