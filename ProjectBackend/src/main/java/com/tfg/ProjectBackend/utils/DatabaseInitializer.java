package com.tfg.ProjectBackend.utils;

import com.tfg.ProjectBackend.model.*;
import com.tfg.ProjectBackend.repository.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import javax.sql.rowset.serial.SerialBlob;
import java.io.IOException;
import java.sql.Blob;
import java.sql.SQLException;

@Service
public class DatabaseInitializer {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private TruckRepository truckRepository;

    @Autowired
    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() throws IOException, SQLException {
        ClassPathResource imgFile = new ClassPathResource("static/img/defaultProductPhoto.jpg");
        byte[] photoBytes = StreamUtils.copyToByteArray(imgFile.getInputStream());
        Blob userBlob = new SerialBlob(photoBytes);

        imgFile = new ClassPathResource("static/img/defaultProfileProto.png");
        photoBytes = StreamUtils.copyToByteArray(imgFile.getInputStream());
        Blob productBlob = new SerialBlob(photoBytes);

        Product product1 = new Product("1A2", "Router portátil", productBlob, "Conectividad en todas partes", 100);
        Product product2 = new Product("2A3", "Televisor", productBlob, "Disfruta de tus series favoritas", 800);
        Product product3 = new Product("3A4", "Smartwatch", productBlob, "Monitoriza fácilmente tu ejercicio", 300);

        String mail = "korex53699@bulmp3.com";
        User user1 = new User("Alonso Gómez","alonsogomez", mail, "CallePorDefecto1", userBlob, passwordEncoder.encode("pass"), "USER");
        User user2 = new User("Sara García","saragarcia", mail, "CallePorDefecto2", userBlob, passwordEncoder.encode("pass"), "DELIVERY");
        User user3 = new User("Miguel Martínez","miguelmartinez", mail, "CallePorDefecto3", userBlob, passwordEncoder.encode("pass"), "ADMIN");

        Truck truck1 = new Truck("2C4RD");
        Truck truck2 = new Truck("5U7TH");

        Order order1 = new Order("23456", truck1, 5000, 192.53f);
        Order order2 = new Order("56789", truck2, 10000, 83.78f);

        order1.getProducts().add(product1);
        order1.getProducts().add(product2);
        order2.getProducts().add(product3);
        order1.setAssignedTruck(truck1);
        order2.setAssignedTruck(truck2);

        user3.getRegisteredOrders().add(order1);
        user1.getRegisteredOrders().add(order2);

        Review review1 = new Review(user1, product1, 5, "Muy buen producto");

        user1.getPublishedReviews().add(review1);
        product1.getReviews().add(review1);

        Shop shop1 = new Shop("52552", "Madrid-Recoletos", "CallePorDefecto4");
        shop1.getAssignedTrucks().add(truck1);
        shop1.getAssignedTrucks().add(truck2);
        shop1.getAvailableProducts().add(product1);
        shop1.getAvailableProducts().add(product2);

        productRepository.save(product1);
        productRepository.save(product2);
        productRepository.save(product3);

        userRepository.save(user1);
        userRepository.save(user2);
        userRepository.save(user3);

        truckRepository.save(truck1);
        truckRepository.save(truck2);

        orderRepository.save(order1);
        orderRepository.save(order2);

        shopRepository.save(shop1);

        reviewRepository.save(review1);
    }



}
