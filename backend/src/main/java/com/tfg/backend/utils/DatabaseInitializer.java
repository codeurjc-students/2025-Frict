package com.tfg.backend.utils;

import com.tfg.backend.model.*;
import com.tfg.backend.repository.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import javax.sql.rowset.serial.SerialBlob;
import java.io.IOException;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

@Service
public class DatabaseInitializer {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private TruckRepository truckRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.db.init}")
    private boolean initEnabled;

    @PostConstruct
    public void init() throws IOException, SQLException {
        if (initEnabled) {
            // 1. Default images
            ClassPathResource defaultProductImgFile = new ClassPathResource("static/img/defaultProductImage.jpg");
            byte[] defaultProductPhotoBytes = StreamUtils.copyToByteArray(defaultProductImgFile.getInputStream());
            Blob defaultProductBlob = new SerialBlob(defaultProductPhotoBytes);

            ClassPathResource defaultCategoryImgFile = new ClassPathResource("static/img/defaultCategoryImage.jpg");
            byte[] defaultCategoryPhotoBytes = StreamUtils.copyToByteArray(defaultCategoryImgFile.getInputStream());
            Blob defaultCategoryBlob = new SerialBlob(defaultCategoryPhotoBytes);

            // 2. Independent entities
            User user1 = userRepository.save(new User("Usuario", "user", "user@gmail.com", "CallePorDefecto1", passwordEncoder.encode("pass"), "USER"));
            User user2 = userRepository.save(new User("Administrador", "admin", "admin@gmail.com", "CallePorDefecto2", passwordEncoder.encode("adminpass"), "ADMIN"));

            Category category1 = categoryRepository.save(new Category("Televisión e Imagen", defaultCategoryBlob));
            Category category2 = categoryRepository.save(new Category("Periféricos", defaultCategoryBlob));

            Product product1 = new Product("1A2", "Router portátil", defaultProductBlob, "Conectividad en todas partes", 100);
            Product product2 = new Product("2A3", "Televisor", defaultProductBlob, "Disfruta de tus series favoritas", 800);
            Product product3 = new Product("3A4", "Smartwatch", defaultProductBlob, "Monitoriza fácilmente tu ejercicio", 300);

            product1.setCategories(Set.of(category1, category2));
            product2.setCategories(Set.of(category1));
            product3.setCategories(Set.of(category2));

            product1 = productRepository.save(product1);
            product2 = productRepository.save(product2);
            product3 = productRepository.save(product3);

            Shop shop1 = new Shop("52552", "Madrid-Recoletos", "CallePorDefecto4");
            shop1 = shopRepository.save(shop1);

            Truck truck1 = new Truck("2C4RD");
            Truck truck2 = new Truck("5U7TH");
            truck1.setAssignedShop(shop1);
            truck2.setAssignedShop(shop1);

            truck1 = truckRepository.save(truck1);
            truck2 = truckRepository.save(truck2);

            shop1.getAssignedTrucks().add(truck1);
            shop1.getAssignedTrucks().add(truck2);
            shopRepository.save(shop1);

            Order order1 = new Order("23456", truck1, user1, 5000, 192.53f);
            Order order2 = new Order("56789", truck2, user2, 10000, 83.78f);

            order1.setProducts(new HashSet<>());
            order1.getProducts().add(product1);
            order1.getProducts().add(product2);

            order2.setProducts(new HashSet<>());
            order2.getProducts().add(product1);
            order2.getProducts().add(product3);

            orderRepository.save(order1);
            orderRepository.save(order2);

            user1.getRegisteredOrders().add(order1);
            user2.getRegisteredOrders().add(order2);
            userRepository.save(user1);
            userRepository.save(user2);

            product1.setShopsWithStock(new HashSet<>());
            product2.setShopsWithStock(new HashSet<>());
            product1.getShopsWithStock().add(shop1);
            product2.getShopsWithStock().add(shop1);
            productRepository.save(product1);
            productRepository.save(product2);

            shop1.setAvailableProducts(new HashSet<>());
            shop1.getAvailableProducts().add(product1);
            shop1.getAvailableProducts().add(product2);
            shopRepository.save(shop1);

            Review review1 = new Review(user1, product1, 5, "Muy buen producto");
            user1.setPublishedReviews(new HashSet<>());
            user1.getPublishedReviews().add(review1);

            reviewRepository.save(review1);
            product1.getReviews().add(review1);
            userRepository.save(user1);
            productRepository.save(product1);

            System.out.println("Base de datos inicializada con datos por defecto.");
        }
    }
}
