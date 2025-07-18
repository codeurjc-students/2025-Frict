package com.tfg.backend.model;

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

    private String address;

    @ManyToMany(mappedBy = "shopsWithStock")
    private Set<Product> availableProducts = new HashSet<>();

    @OneToMany(mappedBy = "assignedShop")
    private Set<Truck> assignedTrucks = new HashSet<>();

    public Shop() {
    }

    public Shop(String referenceCode, String name, String address) {
        this.referenceCode = referenceCode;
        this.name = name;
        this.address = address;
    }
}
