package com.tfg.backend.utils;

import com.tfg.backend.model.*;
import com.tfg.backend.repository.*;
import com.tfg.backend.service.StorageService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DatabaseInitializer {

    @Autowired private OrderRepository orderRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private ShopRepository shopRepository;
    @Autowired private TruckRepository truckRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ShopStockRepository shopStockRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private StorageService storageService;

    // Read a list separated by commas. Default: empty list
    @Value("#{'${app.db.init:}'.split(',')}")
    private List<String> initEntities;

    // Static resources defined as fields
    private final ClassPathResource defaultProductResource = new ClassPathResource("static/img/defaultProductImage.jpg");
    private final ClassPathResource defaultCategoryResource = new ClassPathResource("static/img/defaultCategoryImage.jpg");
    private final ClassPathResource defaultUserResource = new ClassPathResource("static/img/defaultUserImage.jpg");

    @PostConstruct
    @Transactional
    public void init() {
        // Critical: Initialize Global Defaults first (Static Holder Pattern)
        // This ensures GlobalDefaults.USER_IMAGE, etc., are available for the whole app and for the data loading below.
        initGlobalImages();

        // Normalize input (trim blank spaces and lower cases)
        List<String> entities = initEntities.stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .toList();

        // Exit is the list is empty or "none"
        if (entities.isEmpty() || (entities.size() == 1 && (entities.getFirst().isEmpty() || entities.getFirst().equals("none")))) {
            return;
        }

        log.info("---- STARTING DATA LOADING FOR: {} ----", entities);

        boolean loadAll = entities.contains("all");

        // The order matters for the foreign keys
        if (loadAll || entities.contains("users")) {
            initUsers();
        }

        if (loadAll || entities.contains("categories")) {
            initCategories();
        }

        if (loadAll || entities.contains("products")) {
            initProducts();
        }

        if (loadAll || entities.contains("shops")) {
            initShopsAndTrucks();
        }

        if (loadAll || entities.contains("orders")) {
            initOrdersAndCart();
        }

        if (loadAll || entities.contains("stock")) {
            initStock();
        }

        if (loadAll || entities.contains("reviews")) {
            initReviews();
        }

        log.info("---- DATABASE INITIALIZATION FINISHED ----");

    }

    private void initGlobalImages() {
        log.info(">>> Uploading Global Default Images to S3...");
        // Upload once, assign to Static Holder
        GlobalDefaults.USER_IMAGE = uploadDefaultImage(defaultUserResource, "users");
        GlobalDefaults.CATEGORY_IMAGE = uploadDefaultImage(defaultCategoryResource, "categories");
        GlobalDefaults.PRODUCT_IMAGE = uploadDefaultImage(defaultProductResource, "products");
    }

    // --------------------------------------------------------------------------------
    // INITIALIZATION METHODS FOR EACH ENTITY
    // --------------------------------------------------------------------------------

    private void initUsers() {
        if (userRepository.count() > 0) return;
        log.info(">>> Initializing Users...");

        User user1 = new User("Usuario", "user", "wekax56917@cucadas.com", "234567890", passwordEncoder.encode("pass"), "USER");
        PaymentCard paymentCard = new PaymentCard("Tarjeta personal", "Carlos López", "1234567890123456", "123", YearMonth.of(2027, 3));
        PaymentCard paymentCard2 = new PaymentCard("Tarjeta trabajo", "María Sánchez", "2345678901234567", "234", YearMonth.of(2028, 5));
        Address address = new Address("Casa","Calle de Ejemplo", "1", "3ºC", "12345", "Ciudad de Ejemplo", "España");
        Address address2 = new Address("Trabajo","Dirección del trabajo", "8", "", "23456", "Ciudad de Ejemplo", "España");

        user1.getCards().add(paymentCard);
        user1.getCards().add(paymentCard2);
        user1.getAddresses().add(address);
        user1.getAddresses().add(address2);

        // Assign GLOBAL default image
        user1.setUserImage(GlobalDefaults.USER_IMAGE);

        userRepository.save(user1);

        User user2 = new User("Administrador", "admin", "admin@gmail.com", "123456789", passwordEncoder.encode("adminpass"), "ADMIN");
        PaymentCard paymentCard3 = new PaymentCard("Tarjeta de la empresa", "Laura Miño", "1233453212231346", "345", YearMonth.of(2028, 7));
        Address address3 = new Address("Casa","Calle del Ciudadano", "18", "3ºC", "34567", "Ciudad de Ejemplo", "España");
        user2.getCards().add(paymentCard3);
        user2.getAddresses().add(address3);

        // Assign GLOBAL default image
        user2.setUserImage(GlobalDefaults.USER_IMAGE);

        userRepository.save(user2);
    }

    private void initCategories() {
        if (categoryRepository.count() > 0) return;
        log.info(">>> Initializing Categories...");

        List<Category> categories = new ArrayList<>();
        categories.add(new Category(
                "Gaming y PC",
                "Domina el juego con el mejor hardware",
                "Componentes, portátiles gaming y periféricos de alto rendimiento.",
                "Encuentra las últimas tarjetas gráficas, procesadores extremos y todo lo necesario para montar tu setup gaming definitivo. Rendimiento sin límites para jugadores exigentes."
        ));

        categories.add(new Category(
                "Almacenamiento",
                "Más espacio, más velocidad",
                "Discos duros, SSDs NVMe y memorias USB.",
                "No te quedes sin espacio. Descubre nuestra gama de soluciones de almacenamiento interno y externo para guardar tus archivos, juegos y copias de seguridad con la máxima fiabilidad."
        ));

        categories.add(new Category(
                "Conectividad y Redes",
                "Conexión estable y rápida",
                "Routers, switches, adaptadores WiFi y cableado.",
                "Mejora la velocidad de tu internet y elimina las zonas muertas. Equipos de red de última generación para tu hogar u oficina inteligente."
        ));

        categories.add(new Category(
                "Energía y Carga",
                "Energía para todo el día",
                "Cargadores, powerbanks, regletas y SAIs.",
                "Mantén tus dispositivos siempre encendidos y protegidos. Soluciones de carga rápida y protección eléctrica contra picos de tensión para tus equipos valiosos."
        ));

        categories.add(new Category(
                "Móviles y Tablets",
                "Tecnología en tu mano",
                "Smartphones, tablets y accesorios imprescindibles.",
                "Las últimas novedades en telefonía móvil. Desde buques insignia hasta opciones calidad-precio, junto con fundas y protectores para mantenerlos como nuevos."
        ));

        categories.add(new Category(
                "Audio y Sonido",
                "Siente cada nota",
                "Auriculares, altavoces, micrófonos y barras de sonido.",
                "Sumérgete en un sonido de alta fidelidad. Equipos de audio profesional y de consumo para disfrutar de tu música, películas y conferencias con claridad cristalina."
        ));

        categories.add(new Category(
                "Hogar Inteligente",
                "Tu casa, ahora más lista",
                "Domótica, asistentes de voz y seguridad smart.",
                "Automatiza tu vida. Controla luces, enchufes y cámaras desde tu móvil. Crea rutinas inteligentes para ahorrar energía y mejorar tu comodidad."
        ));

        categories.add(new Category(
                "Fotografía y Video",
                "Captura el momento perfecto",
                "Cámaras, objetivos, trípodes y estabilizadores.",
                "Equipo para creadores de contenido y fotógrafos. Inmortaliza tus recuerdos con la mejor calidad de imagen, ya seas aficionado o profesional."
        ));

        categories.add(new Category(
                "Televisión e Imagen",
                "Cine en tu salón",
                "Smart TVs 4K, proyectores y monitores.",
                "Disfruta de tus series y películas favoritas con la mejor calidad visual. Pantallas con tecnologías OLED y QLED para colores vibrantes y negros puros."
        ));

        categories.add(new Category(
                "Periféricos",
                "Completa tu experiencia",
                "Teclados, ratones, webcams e impresoras.",
                "Mejora tu productividad y ergonomía. Accesorios esenciales que conectan contigo y tu ordenador para trabajar o estudiar mejor."
        ));

        categories.add(new Category(
                "Software y Servicios",
                "Potencia digital",
                "Sistemas operativos, antivirus y ofimática.",
                "Las licencias originales que necesitas. Protege tu equipo contra virus y aumenta tu productividad con las suites de oficina más populares."
        ));

        categories.add(new Category(
                "Herramientas y Accesorios",
                "Mantenimiento y montaje",
                "Kits de herramientas, pasta térmica y limpieza.",
                "Todo lo necesario para el entusiasta del hardware. Herramientas de precisión para montar PCs y productos de limpieza para cuidar tu electrónica."
        ));

        categories.add(new Category(
                "Recomendado",
                "Nuestra selección experta",
                "Productos elegidos por su calidad y precio.",
                "Una selección curada por nuestros especialistas. Artículos que destacan por su excelente relación calidad-precio y fiabilidad comprobada."
        ));

        categories.add(new Category(
                "Destacado",
                "Tendencias del momento",
                "Lo más nuevo y relevante del mercado.",
                "Descubre los productos que están marcando tendencia ahora mismo. Lanzamientos recientes y artículos que están dando de qué hablar."
        ));

        categories.add(new Category(
                "Top ventas",
                "Los favoritos de la comunidad",
                "Los productos más comprados por nuestros clientes.",
                "Éxito garantizado. Estos son los artículos número uno en ventas, avalados por miles de compras y clientes satisfechos."
        ));

        for (Category cat : categories) {
            // Assign GLOBAL default image (Pointer copy, no new upload)
            cat.setCategoryImage(GlobalDefaults.CATEGORY_IMAGE);
            categoryRepository.save(cat);
        }
    }

    private void initProducts() {
        if (productRepository.count() > 0) return;
        log.info(">>> Initializing Products...");

        List<Product> products = new ArrayList<>();
        products.add(new Product("Smartphone Plegable X", "Innovación en diseño y potencia", 750.00));
        products.getFirst().setPreviousPrice(1000.00);
        products.add(new Product("Laptop Ultradelgada 13\"", "Máxima portabilidad y rendimiento", 1250.50));
        products.get(1).setPreviousPrice(1500.00);
        products.add(new Product("Tarjeta Gráfica RTX 5080", "Gráficos de siguiente generación para gaming", 780.25));
        products.add(new Product("Router WiFi 6E Mesh", "Cobertura total y velocidad Gigabit", 185.70));
        products.add(new Product("Monitor Curvo Ultrawide", "Experiencia inmersiva para profesionales", 499.00));
        products.add(new Product("Cámara Mirrorless 4K", "Fotografía y video de alta resolución", 1120.40));
        products.add(new Product("Disco SSD NVMe 2TB", "Velocidad extrema de lectura/escritura", 155.99));
        products.add(new Product("Teclado Mecánico RGB", "Switches táctiles para gamers y coders", 89.65));
        products.add(new Product("Altavoz Inteligente con IA", "Asistente de voz y sonido premium", 75.30));
        products.add(new Product("Auriculares con Cancelación de Ruido", "Inmersión total en música y llamadas", 199.50));
        products.add(new Product("Smartwatch con ECG", "Monitor de salud avanzado en tu muñeca", 220.00));
        products.add(new Product("Drone Plegable con GPS", "Tomas aéreas estables y de calidad", 345.80));
        products.add(new Product("Batería Externa USB-PD 65W", "Carga tu laptop y móvil en cualquier lugar", 55.45));
        products.add(new Product("Lector de Ebooks con Luz", "Miles de libros sin fatiga visual", 129.90));
        products.add(new Product("Sistema de Alarma Inteligente", "Seguridad para el hogar con control remoto", 240.75));
        products.add(new Product("Convertidor HDMI a USB-C", "Conecta tu laptop a cualquier pantalla", 18.25));
        products.add(new Product("Mini PC Industrial", "Potencia y tamaño reducido para automatización", 510.10));
        products.add(new Product("Gafas de Realidad Mixta", "El futuro de la interacción digital y el trabajo", 2400.00));
        products.add(new Product("Tableta Gráfica Pro 16\"", "Precisión y sensibilidad para el diseño", 390.60));
        products.add(new Product("Impresora 3D de Resina", "Crea prototipos de alta definición en casa", 425.99));
        products.add(new Product("Estación de Carga Inalámbrica Triple", "Carga rápida para tus tres dispositivos Apple/Android", 45.00));
        products.add(new Product("Extensor de Rango Powerline", "Red estable a través de la instalación eléctrica", 68.35));
        products.add(new Product("Consola de Juegos Portátil", "Juegos AAA en tus manos, donde vayas", 450.70));
        products.add(new Product("Sensor de Humedad y Temperatura IoT", "Monitorización ambiental a distancia", 12.88));
        products.add(new Product("Tarjeta de Sonido Externa USB", "Audio de estudio para PC o laptop", 79.95));
        products.add(new Product("Cable Ethernet Cat 8", "Máxima velocidad para redes cableadas", 15.15));
        products.add(new Product("Ventilador de Laptop con RGB", "Refrigeración eficiente para sesiones largas", 29.50));
        products.add(new Product("Kit de Raspberry Pi 5 Avanzado", "Microcomputadora para proyectos de electrónica", 85.60));
        products.add(new Product("Medidor de Calidad de Aire Digital", "Monitorea CO2 y partículas en tiempo real", 115.20));
        products.add(new Product("Sistema de Iluminación Inteligente", "Control de color y brillo por voz o app", 60.99));

        List<Category> categories = categoryRepository.findAll();
        if (!categories.isEmpty()){
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
        }

        // Upload image for each product and save
        for (Product p : products) {
            // New Logic: Create ProductImageInfo using GlobalDefaults data
            // We reuse the URL and Key from the global upload.
            ProductImageInfo pImage = new ProductImageInfo(
                    GlobalDefaults.PRODUCT_IMAGE.getImageUrl(),
                    GlobalDefaults.PRODUCT_IMAGE.getS3Key(),
                    GlobalDefaults.PRODUCT_IMAGE.getFileName(),
                    p
            );
            p.getImages().add(pImage);
            productRepository.save(p);
        }
    }

    private void initShopsAndTrucks() {
        if (shopRepository.count() > 0) return;
        log.info(">>> Initializing Shops and Trucks...");

        Address address1 = new Address("Madrid-Recoletos", "CallePorDefecto4", "3", "", "28900", "Madrid", "España");
        Shop shop1 = shopRepository.save(new Shop("52552", "Madrid-Recoletos", address1));

        Truck truck1 = truckRepository.save(new Truck("2C4RD"));
        Truck truck2 = truckRepository.save(new Truck("5U7TH"));
        truck1.setAssignedShop(shop1);
        truck2.setAssignedShop(shop1);
        truckRepository.save(truck1);
        truckRepository.save(truck2);
    }

    private void initOrdersAndCart() {
        if (orderRepository.count() > 0) return;
        log.info(">>> Initializing Orders and Cart...");

        User user1 = userRepository.findByUsername("user").orElse(null);
        User user2 = userRepository.findByUsername("admin").orElse(null);
        List<Product> products = productRepository.findAll();

        if (user1 == null || user2 == null || products.isEmpty()) {
            log.error("CANNOT INIT ORDERS: Users or Products missing in DB.");
            return;
        }

        List<OrderItem> orderItems1 = new ArrayList<>();
        List<OrderItem> orderItems2 = new ArrayList<>();

        if (products.size() >= 8) {
            orderItems1.add(new OrderItem(products.get(0), user1, 12));
            orderItems1.add(new OrderItem(products.get(2), user1, 3));
            orderItems2.add(new OrderItem(products.get(3), user2, 2));
            orderItems2.add(new OrderItem(products.get(7), user2, 1));

            Order order1 = new Order(user1, orderItems1, user1.getAddresses().getFirst(), user1.getCards().getFirst());
            Order order2 = new Order(user2, orderItems2, user2.getAddresses().getFirst(), user2.getCards().getFirst());
            order1.addStatusUpdate("El pedido ha quedado registrado correctamente en la tienda asignada");
            order1.changeOrderStatus(OrderStatus.SENT, "El pedido se está procesando");

            orderRepository.save(order1);
            orderRepository.save(order2);
        }

        // Cart products
        if (products.size() >= 29) {
            List<OrderItem> cartItems = new ArrayList<>();
            cartItems.add(new OrderItem(products.get(0), user1, 12));
            cartItems.add(new OrderItem(products.get(2), user1, 3));
            cartItems.add(new OrderItem(products.get(3), user1, 1));
            cartItems.add(new OrderItem(products.get(8), user1, 4));
            cartItems.add(new OrderItem(products.get(12), user1, 2));
            cartItems.add(new OrderItem(products.get(15), user1, 3));
            cartItems.add(new OrderItem(products.get(18), user1, 6));
            cartItems.add(new OrderItem(products.get(22), user1, 20));
            cartItems.add(new OrderItem(products.get(23), user1, 15));
            cartItems.add(new OrderItem(products.get(28), user1, 14));
            orderItemRepository.saveAll(cartItems);
        }
    }

    private void initStock() {
        if (shopStockRepository.count() > 0) return;
        log.info(">>> Initializing Stock...");

        List<Shop> shops = shopRepository.findAll();
        List<Product> products = productRepository.findAll();

        if (shops.isEmpty() || products.isEmpty()) {
            log.error("CANNOT INIT STOCK: Shops or Products missing.");
            return;
        }

        Shop shop1 = shops.getFirst();
        List<ShopStock> shopStocks = new ArrayList<>();

        for (int i = 0; i < products.size(); i++) {
            shopStocks.add(new ShopStock(shop1, products.get(i), i));
        }
        shopStockRepository.saveAll(shopStocks);
    }

    private void initReviews() {
        if (reviewRepository.count() > 0) return;
        log.info(">>> Initializing Reviews...");

        User user1 = userRepository.findByUsername("user").orElse(null);
        User user2 = userRepository.findByUsername("admin").orElse(null);
        List<Product> products = productRepository.findAll();

        if (user1 != null && user2 != null && !products.isEmpty()) {
            reviewRepository.save(new Review(user1, products.getFirst(), 5, "Muy buen producto", true));
            reviewRepository.save(new Review(user2, products.getFirst(), 2, "Desastroso", false));
        }
    }

    // --- AUXILIARY METHODS ---
    private ImageInfo uploadDefaultImage(ClassPathResource resource, String folder) {
        try {
            Map<String, String> result = uploadToMinio(resource, resource.getFilename(), folder);
            return new ImageInfo(result.get("url"), result.get("key"), resource.getFilename());
        } catch (IOException e) {
            throw new RuntimeException("CRITICAL: Error uploading default image: " + resource.getFilename(), e);
        }
    }

    private Map<String, String> uploadToMinio(ClassPathResource resource, String originalName, String folder) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            return storageService.uploadFile(
                    inputStream,
                    originalName,
                    "image/jpeg",
                    resource.contentLength(),
                    folder
            );
        }
    }
}