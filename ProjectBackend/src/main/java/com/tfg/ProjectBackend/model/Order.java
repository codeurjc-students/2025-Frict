package com.tfg.ProjectBackend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "products")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String referenceCode;

    @ManyToOne
    private User user;

    @ManyToMany
    private Set<Product> products;

    @ManyToOne
    private Truck assignedTruck;

    private int estimatedCompletionTime;

    private float totalAmount;

    public Order() {
    }

    public Order(String referenceCode, Truck assignedTruck, int estimatedCompletionTime, float totalAmount) {
        this.referenceCode = referenceCode;
        this.assignedTruck = assignedTruck;
        this.estimatedCompletionTime = estimatedCompletionTime;
        this.totalAmount = totalAmount;
    }
}
