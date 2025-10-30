package com.tfg.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tfg.backend.utils.PhotoUtils;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Blob;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
	private List<String> roles;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String address;

    @Lob
    @Column(nullable = false)
    private Blob profilePhoto = PhotoUtils.setDefaultPhoto(User.class);

    private boolean isBanned = false;

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
        this.roles = List.of(roles);
        this.email = email;
        this.address = address;
    }
}