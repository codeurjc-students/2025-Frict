package com.tfg.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tfg.backend.utils.GlobalDefaults;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.*;
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
	private Set<String> roles = new HashSet<>();

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

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> allOrderItems = new ArrayList<>(); //Necessary in order to be able to see the user cart

    @ManyToMany
    private Set<Product> favouriteProducts = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Order> registeredOrders = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Review> publishedReviews = new HashSet<>();

    //For MANAGER role users: Allows them to manage this shops
    @OneToMany(mappedBy = "assignedManager")
    private List<Shop> assignedShops = new ArrayList<>();

    //For USER role users: Allows them to choose the shop in which to place their orders
    @ManyToOne
    private Shop selectedShop;

    //For DRIVER role users: Allows them to manage this truck assigned orders
    @OneToOne(mappedBy = "assignedDriver")
    private Truck assignedTruck;

	public User() {
	}

    public String getRole(){ return roles.stream().findFirst().orElse(null); }

    public boolean hasRole(String roleName) {
        return roles != null && roles.contains(roleName);
    }

    public User(String name, String username, String email, String encodedPassword, String... roles) {
        this.name = name;
        this.username = username;
        this.encodedPassword = encodedPassword;
        this.roles = new HashSet<>(Arrays.asList(roles));
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

    @PrePersist
    protected void onCreate() {
        if (this.userImage == null) {
            this.userImage = GlobalDefaults.getDefaultUserImage();
        }
    }
}