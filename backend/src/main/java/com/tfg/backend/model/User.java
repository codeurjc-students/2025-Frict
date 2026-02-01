package com.tfg.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
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

    private String otpCode = null; //If OTP is not null, the user account is in process of being recovered

    private LocalDateTime otpExpiration;

	@ElementCollection(fetch = FetchType.EAGER) //Mandatory for JWT to work properly
	private Set<String> roles;

    @Column(nullable = false)
    private String email;

    private String phone;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private List<Address> addresses = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private List<PaymentCard> cards = new ArrayList<>();

    @Embedded
    private ImageInfo userImage;

    @Column(nullable = false)
    private boolean isBanned = false;

    @Column(nullable = false)
    private boolean isDeleted = false;

    @Column(nullable = false)
    private boolean isLogged = false;

    private LocalDateTime lastConnection = null;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> allOrderItems = new ArrayList<>(); //Necessary in order to be able to see the user cart

    @ManyToMany
    private Set<Product> favouriteProducts = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Order> registeredOrders = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Review> publishedReviews = new HashSet<>();

    @OneToOne(mappedBy = "assignedManager")
    private Shop assignedShop;

    @OneToOne(mappedBy = "assignedDriver")
    private Truck assignedTruck;

	public User() {
	}

    public User(String name, String username, String email, String encodedPassword, String... roles) {
        this.name = name;
        this.username = username;
        this.encodedPassword = encodedPassword;
        this.roles = Set.of(roles);
        this.email = email;
    }

    //Retrieves only the items that are currently in user cart
    public List<OrderItem> getItemsInCart() {
        return this.allOrderItems.stream()
            .filter(item -> item.getOrder() == null && item.getUser().getId().equals(this.id))
            .collect(Collectors.toList());
    }

    public boolean isOtpValid(String inputCode) {
        if (this.otpCode == null || this.otpExpiration == null) return false;

        // Input and stored OTP match, and stored OTP has not expired yet
        return this.otpCode.equals(inputCode) && LocalDateTime.now().isBefore(this.otpExpiration);
    }
}