package com.tfg.backend.utils;

import com.tfg.backend.model.*;
import com.tfg.backend.repository.*;
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
import java.util.HashSet; // Importar HashSet

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

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() throws IOException, SQLException {

        // 1. Fotos por defecto
        ClassPathResource defaultProductImgFile = new ClassPathResource("static/img/defaultProductPhoto.jpg");
        byte[] defaultProductPhotoBytes = StreamUtils.copyToByteArray(defaultProductImgFile.getInputStream());
        Blob defaultProductBlob = new SerialBlob(defaultProductPhotoBytes);

        ClassPathResource defaultProfileImgFile = new ClassPathResource("static/img/defaultProfilePhoto.jpg");
        byte[] defaultProfilePhotoBytes = StreamUtils.copyToByteArray(defaultProfileImgFile.getInputStream());
        Blob defaultProfileBlob = new SerialBlob(defaultProfilePhotoBytes);


        // 2. Entidades "padre" o independientes
        // Usuarios
        User user1 = new User("Alonso Gómez", "alonsogomez", "korex53699@bulmp1.com", "CallePorDefecto1", defaultProfileBlob, passwordEncoder.encode("pass"), "USER");
        User user2 = new User("Sara García", "saragarcia", "korex53699@bulmp2.com", "CallePorDefecto2", defaultProfileBlob, passwordEncoder.encode("pass"), "USER");
        User user3 = new User("Admin", "admin", "admin@gmail.com", "CallePorDefecto3", defaultProfileBlob, passwordEncoder.encode("adminpass"), "ADMIN");

        user1 = userRepository.save(user1); // Guarda y obtiene la instancia persistida
        user2 = userRepository.save(user2);
        user3 = userRepository.save(user3);

        // Productos
        Product product1 = new Product("1A2", "Router portátil", defaultProductBlob, "Conectividad en todas partes", 100);
        Product product2 = new Product("2A3", "Televisor", defaultProductBlob, "Disfruta de tus series favoritas", 800);
        Product product3 = new Product("3A4", "Smartwatch", defaultProductBlob, "Monitoriza fácilmente tu ejercicio", 300);

        product1 = productRepository.save(product1); // Guarda y obtiene la instancia persistida
        product2 = productRepository.save(product2);
        product3 = productRepository.save(product3);

        // Tiendas (Shop) - Las Tiendas y Camiones tienen una relación ManyToOne y OneToMany
        Shop shop1 = new Shop("52552", "Madrid-Recoletos", "CallePorDefecto4");
        shop1 = shopRepository.save(shop1); // Guarda la tienda primero

        // Camiones (Truck) - ahora pueden referenciar a la tienda persistida
        Truck truck1 = new Truck("2C4RD");
        Truck truck2 = new Truck("5U7TH");

        // Asignar tienda a camiones antes de guardarlos
        truck1.setAssignedShop(shop1);
        truck2.setAssignedShop(shop1);

        truck1 = truckRepository.save(truck1); // Guarda y obtiene la instancia persistida
        truck2 = truckRepository.save(truck2);

        shop1.getAssignedTrucks().add(truck1);
        shop1.getAssignedTrucks().add(truck2);
        shop1 = shopRepository.save(shop1); // Vuelve a guardar la tienda para asegurar la actualización de la colección


        // 3. Entidades que referencian a las anteriores
        // Pedidos (Order) - ahora pueden referenciar a usuarios y camiones persistidos
        Order order1 = new Order("23456", truck1, user1, 5000, 192.53f);
        order1.setUser(user1);
        order1.setAssignedTruck(truck1);

        Order order2 = new Order("56789", truck2, user2, 10000, 83.78f);
        order2.setUser(user2);
        order2.setAssignedTruck(truck2);

        // Las relaciones ManyToMany se manejan añadiendo a la colección
        // Las colecciones deben ser inicializadas en los constructores o al declarar:
        // Por ejemplo, en Order: private Set<Product> products = new HashSet<>();
        order1.setProducts(new HashSet<>()); // Asegura que la colección no sea null si no está inicializada en el constructor
        order1.getProducts().add(product1);
        order1.getProducts().add(product2);

        order2.setProducts(new HashSet<>());
        order2.getProducts().add(product1);
        order2.getProducts().add(product3);

        // Guardar los pedidos
        order1 = orderRepository.save(order1);
        order2 = orderRepository.save(order2);

        // Si quieres que las colecciones en el lado "uno" se actualicen automáticamente
        // O podrías recuperarlos y volverlos a guardar después:
        user1.getRegisteredOrders().add(order1); // Agrega a la colección de la instancia persistida de user1
        user2.getRegisteredOrders().add(order2);
        user1 = userRepository.save(user1); // Vuelve a guardar user1 para persistir la relación de colección
        user2 = userRepository.save(user2);

        // Relación ManyToMany de Product a Shop (shopsWithStock)
        product1.setShopsWithStock(new HashSet<>());
        product2.setShopsWithStock(new HashSet<>());
        product1.getShopsWithStock().add(shop1);
        product2.getShopsWithStock().add(shop1);
        productRepository.save(product1); // Guardar para persistir la relación
        productRepository.save(product2);

        // Relación ManyToMany inversa de Shop a Product (availableProducts)
        shop1.setAvailableProducts(new HashSet<>());
        shop1.getAvailableProducts().add(product1);
        shop1.getAvailableProducts().add(product2);
        shopRepository.save(shop1);


        // Reseñas (Review) - ahora pueden referenciar a usuarios y productos persistidos
        Review review1 = new Review(user1, product1, 5, "Muy buen producto");
        user1.setPublishedReviews(new HashSet<>());
        user1.getPublishedReviews().add(review1);

        // Actualizar colecciones de user y product (ya que no está cubierto por CascadeType.ALL)
        review1 = reviewRepository.save(review1);
        product1.getReviews().add(review1);
        userRepository.save(user1);
        productRepository.save(product1);
    }
}