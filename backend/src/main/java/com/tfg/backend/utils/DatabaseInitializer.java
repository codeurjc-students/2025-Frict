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
    private ShopStockRepository shopStockRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.db.init}")
    private boolean initEnabled;

    @PostConstruct
    public void init() throws IOException, SQLException {
        if (initEnabled) {
            // 1. Default images
            ClassPathResource defaultProductImgFile = new ClassPathResource("static/img/defaultProductImage.jpg");
            byte[] defaultProductImageBytes = StreamUtils.copyToByteArray(defaultProductImgFile.getInputStream());
            Blob defaultProductBlob = new SerialBlob(defaultProductImageBytes);

            ClassPathResource defaultCategoryImgFile = new ClassPathResource("static/img/defaultCategoryImage.jpg");
            byte[] defaultCategoryImageBytes = StreamUtils.copyToByteArray(defaultCategoryImgFile.getInputStream());
            Blob defaultCategoryBlob = new SerialBlob(defaultCategoryImageBytes);

            // 2. Independent entities
            User user1 = userRepository.save(new User("Usuario", "user", "user@gmail.com", "CallePorDefecto1", passwordEncoder.encode("pass"), "USER"));
            User user2 = userRepository.save(new User("Administrador", "admin", "admin@gmail.com", "CallePorDefecto2", passwordEncoder.encode("adminpass"), "ADMIN"));

            // Hardware categories
            Category gaming = categoryRepository.save(new Category("Gaming y PC", defaultCategoryBlob));
            Category storage = categoryRepository.save(new Category("Almacenamiento", defaultCategoryBlob));
            Category connectivity = categoryRepository.save(new Category("Conectividad y Redes", defaultCategoryBlob));
            Category power = categoryRepository.save(new Category("Energía y Carga", defaultCategoryBlob));

            // Devices categories
            Category mobile = categoryRepository.save(new Category("Móviles y Tablets", defaultCategoryBlob));
            Category audio = categoryRepository.save(new Category("Audio y Sonido", defaultCategoryBlob));
            Category smartHome = categoryRepository.save(new Category("Hogar Inteligente", defaultCategoryBlob));
            Category photography = categoryRepository.save(new Category("Fotografía y Video", defaultCategoryBlob));
            Category tvImage = categoryRepository.save(new Category("Televisión e Imagen", defaultCategoryBlob));
            Category peripherals = categoryRepository.save(new Category("Periféricos", defaultCategoryBlob));

            // Software and others categories
            Category software = categoryRepository.save(new Category("Software y Servicios", defaultCategoryBlob));
            Category tools = categoryRepository.save(new Category("Herramientas y Accesorios", defaultCategoryBlob));

            //Will later be managed by the recommendation system
            Category recommended = categoryRepository.save(new Category("Recomendado", defaultCategoryBlob));
            Category featured = categoryRepository.save(new Category("Destacado", defaultCategoryBlob));
            Category topSales = categoryRepository.save(new Category("Top ventas", defaultCategoryBlob));
            
            Product product1 = new Product("A101", "Smartphone Plegable X", defaultProductBlob, "Innovación en diseño y potencia", 999.99);
            Product product2 = new Product("B202", "Laptop Ultradelgada 13\"", defaultProductBlob, "Máxima portabilidad y rendimiento", 1250.50);
            Product product3 = new Product("C303", "Tarjeta Gráfica RTX 5080", defaultProductBlob, "Gráficos de siguiente generación para gaming", 780.25);
            Product product4 = new Product("D404", "Router WiFi 6E Mesh", defaultProductBlob, "Cobertura total y velocidad Gigabit", 185.70);
            Product product5 = new Product("E505", "Monitor Curvo Ultrawide", defaultProductBlob, "Experiencia inmersiva para profesionales", 499.00);
            Product product6 = new Product("F606", "Cámara Mirrorless 4K", defaultProductBlob, "Fotografía y video de alta resolución", 1120.40);
            Product product7 = new Product("G707", "Disco SSD NVMe 2TB", defaultProductBlob, "Velocidad extrema de lectura/escritura", 155.99);
            Product product8 = new Product("H808", "Teclado Mecánico RGB", defaultProductBlob, "Switches táctiles para gamers y coders", 89.65);
            Product product9 = new Product("I909", "Altavoz Inteligente con IA", defaultProductBlob, "Asistente de voz y sonido premium", 75.30);
            Product product10 = new Product("J010", "Auriculares con Cancelación de Ruido", defaultProductBlob, "Inmersión total en música y llamadas", 199.50);
            Product product11 = new Product("K111", "Smartwatch con ECG", defaultProductBlob, "Monitor de salud avanzado en tu muñeca", 220.00);
            Product product12 = new Product("L212", "Drone Plegable con GPS", defaultProductBlob, "Tomas aéreas estables y de calidad", 345.80);
            Product product13 = new Product("M313", "Batería Externa USB-PD 65W", defaultProductBlob, "Carga tu laptop y móvil en cualquier lugar", 55.45);
            Product product14 = new Product("N414", "Lector de Ebooks con Luz", defaultProductBlob, "Miles de libros sin fatiga visual", 129.90);
            Product product15 = new Product("O515", "Sistema de Alarma Inteligente", defaultProductBlob, "Seguridad para el hogar con control remoto", 240.75);
            Product product16 = new Product("P616", "Convertidor HDMI a USB-C", defaultProductBlob, "Conecta tu laptop a cualquier pantalla", 18.25);
            Product product17 = new Product("Q717", "Mini PC Industrial", defaultProductBlob, "Potencia y tamaño reducido para automatización", 510.10);
            Product product18 = new Product("R818", "Gafas de Realidad Mixta", defaultProductBlob, "El futuro de la interacción digital y el trabajo", 2400.00);
            Product product19 = new Product("S919", "Tableta Gráfica Pro 16\"", defaultProductBlob, "Precisión y sensibilidad para el diseño", 390.60);
            Product product20 = new Product("T020", "Impresora 3D de Resina", defaultProductBlob, "Crea prototipos de alta definición en casa", 425.99);
            Product product21 = new Product("U121", "Estación de Carga Inalámbrica Triple", defaultProductBlob, "Carga rápida para tus tres dispositivos Apple/Android", 45.00);
            Product product22 = new Product("V222", "Extensor de Rango Powerline", defaultProductBlob, "Red estable a través de la instalación eléctrica", 68.35);
            Product product23 = new Product("W323", "Consola de Juegos Portátil", defaultProductBlob, "Juegos AAA en tus manos, donde vayas", 450.70);
            Product product24 = new Product("X424", "Sensor de Humedad y Temperatura IoT", defaultProductBlob, "Monitorización ambiental a distancia", 12.88);
            Product product25 = new Product("Y525", "Tarjeta de Sonido Externa USB", defaultProductBlob, "Audio de estudio para PC o laptop", 79.95);
            Product product26 = new Product("Z626", "Cable Ethernet Cat 8", defaultProductBlob, "Máxima velocidad para redes cableadas", 15.15);
            Product product27 = new Product("A727", "Ventilador de Laptop con RGB", defaultProductBlob, "Refrigeración eficiente para sesiones largas", 29.50);
            Product product28 = new Product("B828", "Kit de Raspberry Pi 5 Avanzado", defaultProductBlob, "Microcomputadora para proyectos de electrónica", 85.60);
            Product product29 = new Product("C929", "Medidor de Calidad de Aire Digital", defaultProductBlob, "Monitorea CO2 y partículas en tiempo real", 115.20);
            Product product30 = new Product("D030", "Sistema de Iluminación Inteligente", defaultProductBlob, "Control de color y brillo por voz o app", 60.99);

            product1.setCategories(Set.of(mobile, topSales, featured));
            product2.setCategories(Set.of(gaming, recommended, peripherals));
            product3.setCategories(Set.of(gaming, peripherals, topSales));
            product4.setCategories(Set.of(connectivity, smartHome));
            product5.setCategories(Set.of(tvImage, peripherals, featured));
            product6.setCategories(Set.of(photography, recommended));
            product7.setCategories(Set.of(storage, gaming, topSales));
            product8.setCategories(Set.of(peripherals, gaming, topSales));
            product9.setCategories(Set.of(audio, smartHome, featured));
            product10.setCategories(Set.of(audio, peripherals, topSales));
            product11.setCategories(Set.of(mobile, recommended));
            product12.setCategories(Set.of(photography, tools));
            product13.setCategories(Set.of(power, mobile, tools));
            product14.setCategories(Set.of(mobile, recommended));
            product15.setCategories(Set.of(smartHome, featured));
            product16.setCategories(Set.of(connectivity, tools));
            product17.setCategories(Set.of(gaming, connectivity));
            product18.setCategories(Set.of(gaming, featured, peripherals));
            product19.setCategories(Set.of(peripherals, photography));
            product20.setCategories(Set.of(tools, recommended));
            product21.setCategories(Set.of(power, mobile));
            product22.setCategories(Set.of(connectivity, smartHome));
            product23.setCategories(Set.of(gaming, topSales, featured));
            product24.setCategories(Set.of(smartHome, tools));
            product25.setCategories(Set.of(audio, peripherals));
            product26.setCategories(Set.of(connectivity, tools, topSales));
            product27.setCategories(Set.of(gaming, tools));
            product28.setCategories(Set.of(gaming, smartHome, tools));
            product29.setCategories(Set.of(smartHome, recommended));
            product30.setCategories(Set.of(smartHome, tvImage, featured));

            product1 = productRepository.save(product1);
            product2 = productRepository.save(product2);
            product3 = productRepository.save(product3);
            product4 = productRepository.save(product4);
            product5 = productRepository.save(product5);
            product6 = productRepository.save(product6);
            product7 = productRepository.save(product7);
            product8 = productRepository.save(product8);
            product9 = productRepository.save(product9);
            product10 = productRepository.save(product10);
            product11 = productRepository.save(product11);
            product12 = productRepository.save(product12);
            product13 = productRepository.save(product13);
            product14 = productRepository.save(product14);
            product15 = productRepository.save(product15);
            product16 = productRepository.save(product16);
            product17 = productRepository.save(product17);
            product18 = productRepository.save(product18);
            product19 = productRepository.save(product19);
            product20 = productRepository.save(product20);
            product21 = productRepository.save(product21);
            product22 = productRepository.save(product22);
            product23 = productRepository.save(product23);
            product24 = productRepository.save(product24);
            product25 = productRepository.save(product25);
            product26 = productRepository.save(product26);
            product27 = productRepository.save(product27);
            product28 = productRepository.save(product28);
            product29 = productRepository.save(product29);
            product30 = productRepository.save(product30);

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
            Order order2 = new Order("56789", truck2, user1, 10000, 83.78f);

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
            productRepository.save(product1);
            productRepository.save(product2);

            shop1.setAvailableProducts(new HashSet<>());
            shopRepository.save(shop1);

            ShopStock ss1 = new ShopStock(shop1, product1, 3);
            ShopStock ss1f = new ShopStock(shop1, product1, 4);
            ShopStock ss2 = new ShopStock(shop1, product2, 10);

            shopStockRepository.save(ss1);
            shopStockRepository.save(ss2);

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
