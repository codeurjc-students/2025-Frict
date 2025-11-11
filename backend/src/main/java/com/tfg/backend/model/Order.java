package com.tfg.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

    private int totalProducts = 0; //If totalProducts does not match products list size, it means some of the associated products were deleted

    @ManyToMany
    private List<Product> products = new ArrayList<>(); //Needs to be a list as it must allow saving the same product more than one time

    @ManyToOne
    private Truck assignedTruck;

    private int estimatedCompletionTime;

    private float totalAmount;

    public Order() {
    }

    public Order(String referenceCode, User user, List<Product> products, Truck assignedTruck, int estimatedCompletionTime, float totalAmount) {
        this.referenceCode = referenceCode;
        this.user = user;
        this.products = products;
        this.totalProducts = products.size();
        this.assignedTruck = assignedTruck;
        this.estimatedCompletionTime = estimatedCompletionTime;
        this.totalAmount = totalAmount;
    }
}
