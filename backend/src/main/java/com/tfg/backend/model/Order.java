package com.tfg.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String referenceCode;

    @ManyToOne
    private User user;

    @ManyToMany
    private Set<Product> products = new HashSet<>();

    @ManyToOne
    private Truck assignedTruck;

    private int estimatedCompletionTime;

    private float totalAmount;

    public Order() {
    }

    public Order(String referenceCode, Truck assignedTruck, User user, int estimatedCompletionTime, float totalAmount) {
        this.referenceCode = referenceCode;
        this.assignedTruck = assignedTruck;
        this.user = user;
        this.estimatedCompletionTime = estimatedCompletionTime;
        this.totalAmount = totalAmount;
    }
}
