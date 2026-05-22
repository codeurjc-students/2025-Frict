package com.tfg.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_specs",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
public class ProductSpec {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ElementCollection
    @CollectionTable(name = "product_spec_values", joinColumns = @JoinColumn(name = "spec_id"))
    @Column(name = "value")
    private List<String> values = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    public ProductSpec(String name, List<String> values, Product product) {
        this.name = name;
        this.values = new ArrayList<>(values);
        this.product = product;
    }
}
