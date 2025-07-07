package com.tfg.ProjectBackend.model;

import java.io.IOException;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.*;

import com.tfg.ProjectBackend.utils.PhotoUtils;
import jakarta.persistence.*;
import javax.sql.rowset.serial.SerialBlob;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;


@Setter
@Getter
@Entity
@Table(name = "users")
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

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String address;

    @Lob
    @Column(nullable = false)
    private Blob profilePhoto;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private List<String> role; //Se obliga a que sea un tipo Collection, pero solamente tendrá un rol

    private boolean isBanned;

    @OneToMany(mappedBy = "user")
    private Set<Order> registeredOrders = new HashSet<>();

    @OneToMany(mappedBy = "user")
    private Set<Review> publishedReviews = new HashSet<>();

    public User() {
    }

    public User(String name, String username, String email, String address, Blob profilePhoto, String encodedPassword, String role) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.encodedPassword = encodedPassword;
        this.email = email;
        this.address = address;
        if (profilePhoto!=null){
            this.profilePhoto = profilePhoto;
        } else {
            this.profilePhoto = PhotoUtils.setDefaultPhoto(User.class);
        }
        this.isBanned = false;
        this.role = List.of(role);
        this.registeredOrders = new HashSet<>();
        this.publishedReviews = new HashSet<>();
    }
}
