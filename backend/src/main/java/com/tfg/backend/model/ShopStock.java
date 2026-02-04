package com.tfg.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

//Entity that acts like an intermediate table between Shops and products, in order to include more information fields that concern a Shop and a Product
//This class does not need a dedicated service, as it is handled directly from Shop and Product entities
//It is also possible not to use conventional id by creating a class with an @EmbeddedId, an @Embeddable composed PK class, and @MapsId attributes referencing the composed PK attributed (not used due to its complexity)
@Entity
@Getter
@Setter
@Table(
        uniqueConstraints = @UniqueConstraint(columnNames = {"shop_id", "product_id"}), //Restriction not to allow multiple product-shop assignments
        name = "shops_stocks"
)
public class ShopStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "shop_id")
    private Shop shop;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private int units;

    private boolean active = true;

    public ShopStock() {
    }

    public ShopStock(Shop shop, Product product, int units) {
        this.shop = shop;
        this.product = product;
        this.units = units;
    }
}
