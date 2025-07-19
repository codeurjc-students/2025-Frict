package com.tfg.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "trucks")
public class Truck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String referenceCode;

    @OneToMany(mappedBy = "assignedTruck")
    private Set<Order> ordersToDeliver = new HashSet<>();

    @ManyToOne
    private Shop assignedShop;

    public Truck() {
    }

    public Truck(String referenceCode) {
        this.id = id;
        this.referenceCode = referenceCode;
    }
}
