package com.tfg.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tfg.backend.utils.ImageUtils;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.sql.Blob;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Setter
@Getter
@Entity
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

    private String name;

    @Column(unique = true, nullable = false)
	private String username;

    @JsonIgnore
    @Column(nullable = false)
	private String encodedPassword;

	@ElementCollection(fetch = FetchType.EAGER)
	private Set<String> roles;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String address;

    @Lob
    @Column(nullable = false)
    private Blob profileImage = ImageUtils.prepareDefaultImage(User.class);

    @Column(nullable = false)
    private boolean isBanned = false;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> allOrderItems = new ArrayList<>();

    @ManyToMany
    private Set<Product> favouriteProducts = new HashSet<>();

    @OneToMany(mappedBy = "user")
    private Set<Order> registeredOrders = new HashSet<>();

    @OneToMany(mappedBy = "user")
    private Set<Review> publishedReviews = new HashSet<>();

	public User() {
	}

    public User(String name, String username, String email, String address, String encodedPassword, String... roles) {
        this.name = name;
        this.username = username;
        this.encodedPassword = encodedPassword;
        this.roles = Set.of(roles);
        this.email = email;
        this.address = address;
    }

    public List<OrderItem> getItemsInCart() {
        return this.allOrderItems.stream()
            .filter(item -> item.getOrder() == null && item.getUser().getId().equals(this.id))
            .collect(Collectors.toList());
    }
}