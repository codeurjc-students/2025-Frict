package com.tfg.backend.utils;

import com.tfg.backend.model.*;
import com.tfg.backend.repository.*;
import com.tfg.backend.service.StorageService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.YearMonth;
import java.util.*;

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
    @Autowired
    private StorageService storageService;

    @Value("${app.db.init}")
    private boolean initEnabled;

    @PostConstruct
    public void init() {
        if (initEnabled) {
            System.out.println("---- STARTING DATA LOADING AND IMAGE UPLOAD TO MINIO (STREAMING) ----");

            // 1. Load static resources
            ClassPathResource defaultProductRes = new ClassPathResource("static/img/defaultProductImage.jpg");
            ClassPathResource defaultCategoryRes = new ClassPathResource("static/img/defaultCategoryImage.jpg");
            ClassPathResource defaultProfileRes = new ClassPathResource("static/img/defaultProfileImage.jpg");

            // --------------------------------------------------------------------------------
            // 2. USERS (With Image - ImageInfo Embedded)
            // --------------------------------------------------------------------------------
            User user1 = new User("Usuario", "user", "user@gmail.com", passwordEncoder.encode("pass"), "USER");
            asignarAvatar(user1, defaultProfileRes);
            user1 = userRepository.save(user1);

            User user2 = new User("Administrador", "admin", "admin@gmail.com", passwordEncoder.encode("adminpass"), "ADMIN");
            asignarAvatar(user2, defaultProfileRes);
            user2 = userRepository.save(user2);

            PaymentCard paymentCard = new PaymentCard("Usuario 1", "1234567890123456", "000", YearMonth.of(2027, 3));
            user1.getCards().add(paymentCard);
            userRepository.save(user1);

            // --------------------------------------------------------------------------------
            // 3. CATEGORY (With Image - ImageInfo Embedded)
            // --------------------------------------------------------------------------------
            List<Category> categories = new ArrayList<>();
            categories.add(new Category("Gaming y PC"));
            categories.add(new Category("Almacenamiento"));
            categories.add(new Category("Conectividad y Redes"));
            categories.add(new Category("Energía y Carga"));
            categories.add(new Category("Móviles y Tablets"));
            categories.add(new Category("Audio y Sonido"));
            categories.add(new Category("Hogar Inteligente"));
            categories.add(new Category("Fotografía y Video"));
            categories.add(new Category("Televisión e Imagen"));
            categories.add(new Category("Periféricos"));
            categories.add(new Category("Software y Servicios"));
            categories.add(new Category("Herramientas y Accesorios"));
            categories.add(new Category("Recomendado"));
            categories.add(new Category("Destacado"));
            categories.add(new Category("Top ventas"));

            for (Category cat : categories) {
                asignarImagenCategoria(cat, defaultCategoryRes);
                categoryRepository.save(cat);
            }

            // References for products
            Category gaming = categories.get(0);
            Category storage = categories.get(1);
            Category connectivity = categories.get(2);
            Category power = categories.get(3);
            Category mobile = categories.get(4);
            Category audio = categories.get(5);
            Category smartHome = categories.get(6);
            Category photography = categories.get(7);
            Category tvImage = categories.get(8);
            Category peripherals = categories.get(9);
            Category software = categories.get(10);
            Category tools = categories.get(11);
            Category recommended = categories.get(12);
            Category featured = categories.get(13);
            Category topSales = categories.get(14);

            // --------------------------------------------------------------------------------
            // 4. PRODUCTS (with ProductImage Entity)
            // --------------------------------------------------------------------------------
            List<Product> products = new ArrayList<>();
            products.add(new Product("A101", "Smartphone Plegable X", "Innovación en diseño y potencia", 750.00));
            products.getFirst().setPreviousPrice(1000.00);
            products.add(new Product("B202", "Laptop Ultradelgada 13\"", "Máxima portabilidad y rendimiento", 1250.50));
            products.get(1).setPreviousPrice(1500.00);
            products.add(new Product("C303", "Tarjeta Gráfica RTX 5080", "Gráficos de siguiente generación para gaming", 780.25));
            products.add(new Product("D404", "Router WiFi 6E Mesh", "Cobertura total y velocidad Gigabit", 185.70));
            products.add(new Product("E505", "Monitor Curvo Ultrawide", "Experiencia inmersiva para profesionales", 499.00));
            products.add(new Product("F606", "Cámara Mirrorless 4K", "Fotografía y video de alta resolución", 1120.40));
            products.add(new Product("G707", "Disco SSD NVMe 2TB", "Velocidad extrema de lectura/escritura", 155.99));
            products.add(new Product("H808", "Teclado Mecánico RGB", "Switches táctiles para gamers y coders", 89.65));
            products.add(new Product("I909", "Altavoz Inteligente con IA", "Asistente de voz y sonido premium", 75.30));
            products.add(new Product("J010", "Auriculares con Cancelación de Ruido", "Inmersión total en música y llamadas", 199.50));
            products.add(new Product("K111", "Smartwatch con ECG", "Monitor de salud avanzado en tu muñeca", 220.00));
            products.add(new Product("L212", "Drone Plegable con GPS", "Tomas aéreas estables y de calidad", 345.80));
            products.add(new Product("M313", "Batería Externa USB-PD 65W", "Carga tu laptop y móvil en cualquier lugar", 55.45));
            products.add(new Product("N414", "Lector de Ebooks con Luz", "Miles de libros sin fatiga visual", 129.90));
            products.add(new Product("O515", "Sistema de Alarma Inteligente", "Seguridad para el hogar con control remoto", 240.75));
            products.add(new Product("P616", "Convertidor HDMI a USB-C", "Conecta tu laptop a cualquier pantalla", 18.25));
            products.add(new Product("Q717", "Mini PC Industrial", "Potencia y tamaño reducido para automatización", 510.10));
            products.add(new Product("R818", "Gafas de Realidad Mixta", "El futuro de la interacción digital y el trabajo", 2400.00));
            products.add(new Product("S919", "Tableta Gráfica Pro 16\"", "Precisión y sensibilidad para el diseño", 390.60));
            products.add(new Product("T020", "Impresora 3D de Resina", "Crea prototipos de alta definición en casa", 425.99));
            products.add(new Product("U121", "Estación de Carga Inalámbrica Triple", "Carga rápida para tus tres dispositivos Apple/Android", 45.00));
            products.add(new Product("V222", "Extensor de Rango Powerline", "Red estable a través de la instalación eléctrica", 68.35));
            products.add(new Product("W323", "Consola de Juegos Portátil", "Juegos AAA en tus manos, donde vayas", 450.70));
            products.add(new Product("X424", "Sensor de Humedad y Temperatura IoT", "Monitorización ambiental a distancia", 12.88));
            products.add(new Product("Y525", "Tarjeta de Sonido Externa USB", "Audio de estudio para PC o laptop", 79.95));
            products.add(new Product("Z626", "Cable Ethernet Cat 8", "Máxima velocidad para redes cableadas", 15.15));
            products.add(new Product("A727", "Ventilador de Laptop con RGB", "Refrigeración eficiente para sesiones largas", 29.50));
            products.add(new Product("B828", "Kit de Raspberry Pi 5 Avanzado", "Microcomputadora para proyectos de electrónica", 85.60));
            products.add(new Product("C929", "Medidor de Calidad de Aire Digital", "Monitorea CO2 y partículas en tiempo real", 115.20));
            products.add(new Product("D030", "Sistema de Iluminación Inteligente", "Control de color y brillo por voz o app", 60.99));

            // Assign categories
            products.get(0).setCategories(List.of(mobile, topSales, featured));
            products.get(1).setCategories(List.of(gaming, recommended, peripherals));
            products.get(2).setCategories(List.of(gaming, peripherals, topSales));
            products.get(3).setCategories(List.of(connectivity, smartHome));
            products.get(4).setCategories(List.of(tvImage, peripherals, featured));
            products.get(5).setCategories(List.of(photography, recommended));
            products.get(6).setCategories(List.of(storage, gaming, topSales));
            products.get(7).setCategories(List.of(peripherals, gaming, topSales));
            products.get(8).setCategories(List.of(audio, smartHome, featured));
            products.get(9).setCategories(List.of(audio, peripherals, topSales));
            products.get(10).setCategories(List.of(mobile, recommended));
            products.get(11).setCategories(List.of(photography, tools));
            products.get(12).setCategories(List.of(power, mobile, tools));
            products.get(13).setCategories(List.of(mobile, recommended));
            products.get(14).setCategories(List.of(smartHome, featured));
            products.get(15).setCategories(List.of(connectivity, tools));
            products.get(16).setCategories(List.of(gaming, connectivity));
            products.get(17).setCategories(List.of(gaming, featured, peripherals));
            products.get(18).setCategories(List.of(peripherals, photography));
            products.get(19).setCategories(List.of(tools, recommended));
            products.get(20).setCategories(List.of(power, mobile));
            products.get(21).setCategories(List.of(connectivity, smartHome));
            products.get(22).setCategories(List.of(gaming, topSales, featured));
            products.get(23).setCategories(List.of(smartHome, tools));
            products.get(24).setCategories(List.of(audio, peripherals));
            products.get(25).setCategories(List.of(connectivity, tools, topSales));
            products.get(26).setCategories(List.of(gaming, tools));
            products.get(27).setCategories(List.of(gaming, smartHome, tools));
            products.get(28).setCategories(List.of(smartHome, recommended));
            products.get(29).setCategories(List.of(smartHome, tvImage, featured));

            // Upload image for each product and save
            for (Product p : products) {
                asignarImagenProducto(p, defaultProductRes);
                productRepository.save(p);
            }

            // --------------------------------------------------------------------------------
            // 5. REST OF ENTITIES (Shop, Order, Review...)
            // --------------------------------------------------------------------------------
            Address address1 = new Address("Madrid-Recoletos", "CallePorDefecto4", "3", "", "28900", "Madrid", "España");
            Shop shop1 = shopRepository.save(new Shop("52552", "Madrid-Recoletos", address1));

            Truck truck1 = truckRepository.save(new Truck("2C4RD"));
            Truck truck2 = truckRepository.save(new Truck("5U7TH"));
            truck1.setAssignedShop(shop1);
            truck2.setAssignedShop(shop1);
            truckRepository.save(truck1);
            truckRepository.save(truck2);

            shop1.getAssignedTrucks().add(truck1);
            shop1.getAssignedTrucks().add(truck2);
            shopRepository.save(shop1);

            Order order1 = new Order("23456", user1, truck1);
            Order order2 = new Order("56789", user2, truck1);

            List<OrderItem> orderItems1 = new ArrayList<>();
            orderItems1.add(new OrderItem(order1, products.get(0), user1, 12));
            orderItems1.add(new OrderItem(order1, products.get(2), user1, 3));
            orderItems1.add(new OrderItem(null, products.get(3), user1, 1)); //Item in user1 cart
            orderItems1.add(new OrderItem(null, products.get(8), user1, 4));
            orderItems1.add(new OrderItem(null, products.get(12), user1, 2));
            orderItems1.add(new OrderItem(null, products.get(15), user1, 3));
            orderItems1.add(new OrderItem(null, products.get(18), user1, 6));
            orderItems1.add(new OrderItem(null, products.get(22), user1, 20));
            orderItems1.add(new OrderItem(null, products.get(23), user1, 15));
            orderItems1.add(new OrderItem(null, products.get(28), user1, 14));

            List<OrderItem> orderItems2 = new ArrayList<>();
            orderItems2.add(new OrderItem(order2, products.get(3), user2, 2));
            orderItems2.add(new OrderItem(order2, products.get(7), user2, 1));

            order1.setItems(orderItems1);
            order2.setItems(orderItems2);

            orderRepository.save(order1);
            orderRepository.save(order2);

            user1.getRegisteredOrders().add(order1);
            user2.getRegisteredOrders().add(order2);
            userRepository.save(user1);
            userRepository.save(user2);

            // Stock
            shop1.setAvailableProducts(new HashSet<>());
            shopRepository.save(shop1);

            List<ShopStock> shopStocks = new ArrayList<>();
            shopStocks.add(new ShopStock(shop1, products.get(0), 0));
            shopStocks.add(new ShopStock(shop1, products.get(1), 1));
            shopStocks.add(new ShopStock(shop1, products.get(2), 2));
            shopStocks.add(new ShopStock(shop1, products.get(3), 3));
            shopStocks.add(new ShopStock(shop1, products.get(4), 4));
            shopStocks.add(new ShopStock(shop1, products.get(5), 5));
            shopStocks.add(new ShopStock(shop1, products.get(6), 6));
            shopStocks.add(new ShopStock(shop1, products.get(7), 7));
            shopStocks.add(new ShopStock(shop1, products.get(8), 8));
            shopStocks.add(new ShopStock(shop1, products.get(9), 9));
            shopStocks.add(new ShopStock(shop1, products.get(10), 10));
            shopStocks.add(new ShopStock(shop1, products.get(11), 11));
            shopStocks.add(new ShopStock(shop1, products.get(12), 12));
            shopStocks.add(new ShopStock(shop1, products.get(13), 13));
            shopStocks.add(new ShopStock(shop1, products.get(14), 14));
            shopStocks.add(new ShopStock(shop1, products.get(15), 15));
            shopStocks.add(new ShopStock(shop1, products.get(16), 16));
            shopStocks.add(new ShopStock(shop1, products.get(17), 17));
            shopStocks.add(new ShopStock(shop1, products.get(18), 18));
            shopStocks.add(new ShopStock(shop1, products.get(19), 19));
            shopStocks.add(new ShopStock(shop1, products.get(20), 20));
            shopStocks.add(new ShopStock(shop1, products.get(21), 21));
            shopStocks.add(new ShopStock(shop1, products.get(22), 22));
            shopStocks.add(new ShopStock(shop1, products.get(23), 23));
            shopStocks.add(new ShopStock(shop1, products.get(24), 24));
            shopStocks.add(new ShopStock(shop1, products.get(25), 25));
            shopStocks.add(new ShopStock(shop1, products.get(26), 26));
            shopStocks.add(new ShopStock(shop1, products.get(27), 27));
            shopStocks.add(new ShopStock(shop1, products.get(28), 28));
            shopStocks.add(new ShopStock(shop1, products.get(29), 29));
            shopStockRepository.saveAll(shopStocks);

            reviewRepository.save(new Review(user1, products.get(0), 5, "Muy buen producto", true));
            reviewRepository.save(new Review(user2, products.get(0), 2, "Desastroso", false));

            System.out.println("---- DATABASE SUCCESSFULLY INITIALIZED ----");
        }
    }

    // --- AUXILIARY METHODS ---

    private void asignarAvatar(User user, ClassPathResource resource) {
        try {
            // Use the resource name as file name
            Map<String, String> result = uploadToMinio(resource, resource.getFilename(), "users");
            user.setUserImage(new ImageInfo(result.get("url"), result.get("key"), resource.getFilename()));
        } catch (IOException e) {
            System.err.println("Error subiendo imagen para usuario " + user.getUsername());
        }
    }

    private void asignarImagenCategoria(Category cat, ClassPathResource resource) {
        try {
            Map<String, String> result = uploadToMinio(resource, resource.getFilename(), "categories");
            cat.setCategoryImage(new ImageInfo(result.get("url"), result.get("key"), resource.getFilename()));
        } catch (IOException e) {
            System.err.println("Error subiendo imagen para categoría " + cat.getName());
        }
    }

    private void asignarImagenProducto(Product product, ClassPathResource resource) {
        try {
            Map<String, String> result = uploadToMinio(resource, resource.getFilename(), "products");

            // Use ProductImage (Entity)
            ProductImageInfo pi = new ProductImageInfo(result.get("url"), result.get("key"), resource.getFilename(), product);
            product.getImages().add(pi);

        } catch (IOException e) {
            System.err.println("Error subiendo imagen para producto " + product.getName());
        }
    }

    // Pass the InputStream directly to the updated service
    private Map<String, String> uploadToMinio(ClassPathResource resource, String originalName, String folder) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            return storageService.uploadFile(
                    inputStream,
                    originalName,
                    "image/jpeg", // Assume JPG as image type
                    resource.contentLength(),
                    folder
            );
        }
    }
}