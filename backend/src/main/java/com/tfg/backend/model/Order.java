package com.tfg.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>(); //Needs to be a list as it must allow saving the same product more than one time

    @ManyToOne
    private Truck assignedTruck;

    private int estimatedCompletionTime = 0;

    private double totalAmount = 0.0;

    public Order() {
    }

    public Order(String referenceCode, User user, Truck assignedTruck) {
        this.referenceCode = referenceCode;
        this.user = user;
        this.assignedTruck = assignedTruck;
    }
}
