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
import java.util.Optional;
import java.util.stream.Collectors;

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
    private final ClassPathResource defaultShopResource = new ClassPathResource("static/img/defaultShopImage.jpg");

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
        GlobalDefaults.SHOP_IMAGE = uploadDefaultImage(defaultShopResource, "shops");
    }

    // --------------------------------------------------------------------------------
    // INITIALIZATION METHODS FOR EACH ENTITY
    // --------------------------------------------------------------------------------

    private void initUsers() {
        if (userRepository.count() > 0) return;
        log.info(">>> Initializing Users...");

        User user1 = new User("Usuario", "user", "laxari3928@1200b.com", passwordEncoder.encode("pass"), "USER");
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

        User user2 = new User("Administrador", "admin", "laxari3928@1200b.com", passwordEncoder.encode("adminpass"), "ADMIN");
        PaymentCard paymentCard3 = new PaymentCard("Tarjeta de la empresa", "Laura Miño", "1233453212231346", "345", YearMonth.of(2028, 7));
        Address address3 = new Address("Casa","Calle del Ciudadano", "18", "3ºC", "34567", "Ciudad de Ejemplo", "España");
        user2.getCards().add(paymentCard3);
        user2.getAddresses().add(address3);
        user2.setUserImage(GlobalDefaults.USER_IMAGE);
        userRepository.save(user2);


        User user3 = new User("Gerente", "manager", "manager@gmail.com", passwordEncoder.encode("managerpass"), "MANAGER");
        user3.setUserImage(GlobalDefaults.USER_IMAGE);
        userRepository.save(user3);

        User user4 = new User("Conductor", "driver", "driver@gmail.com", passwordEncoder.encode("driverpass"), "DRIVER");
        user4.setUserImage(GlobalDefaults.USER_IMAGE);
        userRepository.save(user4);
    }

    private void initCategories() {
        if (categoryRepository.count() > 0) return;
        log.info(">>> Initializing Hierarchical Categories...");

        List<Category> roots = new ArrayList<>();

        // --- ROOT CATEGORY 1 ---
        Category ordenadores = new Category("Ordenadores", "pi pi-desktop", "Potencia para todo", "Portátiles, Sobremesa y Mini PCs", "Equipos completos listos para usar.");

        Category portatiles = new Category("Portátiles", "", "Llévalo contigo", "Ultrabooks y Gaming Laptops", "Movilidad sin compromisos.");
        Category sobremesa = new Category("Sobremesa y Mini PC", "", "Máximo rendimiento", "Torres y compactos", "Para oficina o setups minimalistas.");

        ordenadores.addChild(portatiles);
        ordenadores.addChild(sobremesa);
        roots.add(ordenadores);

        // --- ROOT CATEGORY 2 ---
        Category componentes = new Category("Componentes", "pi pi-database", "Monta tu PC", "Hardware interno de alto rendimiento", "El corazón de tu ordenador.");

        Category almacenamiento = new Category("Almacenamiento", "", "Guárdalo todo", "SSD, NVMe y Discos Duros", "Velocidad y capacidad para tus datos.");
        Category graficas = new Category("Tarjetas Gráficas", "", "Gráficos Next-Gen", "GPUs NVIDIA y AMD", "Potencia visual para juegos y renderizado.");
        Category herramientas = new Category("Herramientas y Montaje", "", "Taller PC", "Pastas térmicas y kits", "Todo para el mantenimiento.");

        componentes.addChild(almacenamiento);
        componentes.addChild(graficas);
        componentes.addChild(herramientas);
        roots.add(componentes);

        // --- ROOT CATEGORY 3 ---
        Category perifericos = new Category("Periféricos", "pi pi-wifi", "Tu conexión con el PC", "Teclados, ratones y monitores", "Mejora tu interacción.");

        Category monitores = new Category("Monitores y Pantallas", "", "Visualización perfecta", "Monitores 4K, Curvos y Gaming", "No pierdas detalle.");
        Category entrada = new Category("Teclados y Ratones", "", "Control total", "Mecánicos, inalámbricos y ergonómicos", "Precisión en cada clic.");
        Category audio = new Category("Audio y Sonido", "", "Experiencia sonora", "Auriculares y Altavoces", "Calidad de estudio.");

        perifericos.addChild(monitores);
        perifericos.addChild(entrada);
        perifericos.addChild(audio);
        roots.add(perifericos);

        // --- ROOT CATEGORY 4 ---
        Category movilidad = new Category("Telefonía y Wearables", "pi pi-bolt", "Siempre conectado", "Smartphones y relojes inteligentes", "Tecnología de bolsillo.");

        Category moviles = new Category("Móviles y Smartphones", "", "Última generación", "Android y iOS", "Potencia en tu mano.");
        Category wearables = new Category("Smartwatches", "", "Salud y notificaciones", "Relojes y pulseras", "Tu asistente de muñeca.");

        movilidad.addChild(moviles);
        movilidad.addChild(wearables);
        roots.add(movilidad);

        // --- ROOT CATEGORY 5 ---
        Category hogarRedes = new Category("Hogar y Conectividad", "pi pi-mobile", "Tu espacio inteligente", "Domótica y Redes", "Moderniza tu entorno.");

        Category redes = new Category("Redes y WiFi", "", "Internet veloz", "Routers y Mesh", "Adiós al lag.");
        Category domotica = new Category("Hogar Inteligente", "", "Automatización", "Bombillas y Asistentes", "Controla tu casa por voz.");
        Category energia = new Category("Energía", "", "Poder constante", "Cargadores y Powerbanks", "Baterías siempre llenas.");

        hogarRedes.addChild(redes);
        hogarRedes.addChild(domotica);
        hogarRedes.addChild(energia);
        roots.add(hogarRedes);

        // --- ROOT CATEGORY 6 ---
        Category multimedia = new Category("Foto y Video", "pi pi-headphones", "Creadores de contenido", "Cámaras y Accesorios", "Captura el mundo.");
        roots.add(multimedia);

        roots.add(new Category("Recomendado", "", "Nuestra selección", "Calidad precio", "Elegidos por expertos."));
        roots.add(new Category("Destacado", "", "Tendencias", "Lo más nuevo", "Lo que está de moda."));
        roots.add(new Category("Top Ventas", "", "Los más vendidos", "Favoritos de la comunidad", "Éxito garantizado."));
        roots.add(new Category("Otros", "", "No clasificados o pendientes", "", ""));

        for (Category root : roots) {
            assignCategoryImage(root);
            categoryRepository.save(root);
        }
    }

    private void assignCategoryImage(Category category) {
        category.setCategoryImage(GlobalDefaults.CATEGORY_IMAGE);
        if (category.getChildren() != null) {
            for (Category child : category.getChildren()) {
                assignCategoryImage(child);
            }
        }
    }

    private void initProducts() {
        if (productRepository.count() > 0) return;
        log.info(">>> Initializing Products...");

        // Load category map
        Map<String, Category> catMap = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getName, c -> c));

        List<Product> products = new ArrayList<>();

        // --- COMPUTERS ---
        Product p1 = new Product("Laptop Ultradelgada 13\"", "Máxima portabilidad", 1250.50);
        p1.setPreviousPrice(1500.00);
        assignCategories(p1, catMap, "Portátiles", "Recomendado", "Destacado");
        products.add(p1);

        Product p2 = new Product("Mini PC Industrial", "Potencia compacta", 510.10);
        assignCategories(p2, catMap, "Sobremesa y Mini PC", "Destacado");
        products.add(p2);

        Product p18 = new Product("Portátil Gaming Beast 17\"", "RTX 4090 y i9 de última gen", 3200.00);
        p18.setPreviousPrice(3500.00);
        assignCategories(p18, catMap, "Portátiles", "Top Ventas");
        products.add(p18);

        Product p19 = new Product("All-in-One Oficina Pro", "Pantalla 27\" y diseño limpio", 899.99);
        assignCategories(p19, catMap, "Sobremesa y Mini PC");
        products.add(p19);

        // --- MOBILITY ---
        Product p3 = new Product("Smartphone Plegable X", "Innovación en diseño", 750.00);
        p3.setPreviousPrice(1000.00);
        assignCategories(p3, catMap, "Móviles y Smartphones", "Top Ventas", "Destacado");
        products.add(p3);

        Product p4 = new Product("Smartwatch con ECG", "Salud en tu muñeca", 220.00);
        assignCategories(p4, catMap, "Smartwatches", "Recomendado");
        products.add(p4);

        Product p20 = new Product("Tablet Pro 12.9\"", "Pantalla Liquid Retina XDR", 1100.00);
        assignCategories(p20, catMap, "Móviles y Smartphones", "Destacado");
        products.add(p20);

        // --- COMPONENTS ---
        Product p5 = new Product("Tarjeta Gráfica RTX 5080", "Next Gen Gaming", 780.25);
        assignCategories(p5, catMap, "Tarjetas Gráficas", "Top Ventas", "Destacado");
        products.add(p5);

        Product p6 = new Product("Disco SSD NVMe 2TB", "Velocidad extrema", 155.99);
        assignCategories(p6, catMap, "Almacenamiento", "Recomendado");
        products.add(p6);

        Product p7 = new Product("Pasta Térmica Gold", "Refrigeración eficiente", 12.50);
        assignCategories(p7, catMap, "Herramientas y Montaje");
        products.add(p7);

        Product p21 = new Product("Procesador Intel Core i9", "24 núcleos de potencia pura", 589.90);
        assignCategories(p21, catMap, "Recomendado");
        products.add(p21);

        Product p22 = new Product("Kit RAM DDR5 32GB", "6000MHz RGB", 145.00);
        assignCategories(p22, catMap, "Destacado");
        products.add(p22);

        Product p23 = new Product("Fuente Alimentación 850W", "Certificación Gold Modular", 119.99);
        assignCategories(p23, catMap, "Herramientas y Montaje");
        products.add(p23);

        // --- PERIPHERALS ---
        Product p8 = new Product("Monitor Curvo Ultrawide", "Inmersión total", 499.00);
        assignCategories(p8, catMap, "Monitores y Pantallas", "Destacado");
        products.add(p8);

        Product p9 = new Product("Teclado Mecánico RGB", "Switches táctiles", 89.65);
        assignCategories(p9, catMap, "Teclados y Ratones", "Top Ventas");
        products.add(p9);

        Product p10 = new Product("Auriculares Cancelación Ruido", "Silencio absoluto", 199.50);
        assignCategories(p10, catMap, "Audio y Sonido", "Recomendado");
        products.add(p10);

        Product p24 = new Product("Ratón Gaming Inalámbrico", "Sensor óptico 25K DPI", 79.99);
        p24.setPreviousPrice(99.99);
        assignCategories(p24, catMap, "Teclados y Ratones", "Top Ventas");
        products.add(p24);

        Product p25 = new Product("Webcam StreamCam 1080p", "60fps para creadores", 115.50);
        assignCategories(p25, catMap, "Foto y Video");
        products.add(p25);

        Product p26 = new Product("Micrófono USB Condensador", "Calidad de estudio podcast", 129.99);
        assignCategories(p26, catMap, "Audio y Sonido");
        products.add(p26);

        Product p27 = new Product("Ratón Vertical Ergonómico", "Reduce la fatiga de muñeca", 45.00);
        assignCategories(p27, catMap, "Teclados y Ratones", "Recomendado");
        products.add(p27);

        // --- HOME & NETWORK ---
        Product p11 = new Product("Router WiFi 6E Mesh", "Cobertura total", 185.70);
        assignCategories(p11, catMap, "Redes y WiFi", "Destacado");
        products.add(p11);

        Product p12 = new Product("Altavoz Inteligente IA", "Asistente de hogar", 75.30);
        assignCategories(p12, catMap, "Hogar Inteligente", "Audio y Sonido", "Top Ventas");
        products.add(p12);

        Product p13 = new Product("Batería Externa 65W", "Carga rápida", 55.45);
        assignCategories(p13, catMap, "Energía", "Móviles y Smartphones");
        products.add(p13);

        Product p28 = new Product("Bombilla Inteligente RGB", "Control por voz y app", 15.99);
        assignCategories(p28, catMap, "Hogar Inteligente", "Energía");
        products.add(p28);

        Product p29 = new Product("Switch Gigabit 8 Puertos", "Expansión de red metálica", 25.00);
        assignCategories(p29, catMap, "Redes y WiFi");
        products.add(p29);

        // --- MULTIMEDIA ---
        Product p14 = new Product("Cámara Mirrorless 4K", "Calidad profesional", 1120.40);
        assignCategories(p14, catMap, "Foto y Video", "Destacado");
        products.add(p14);

        Product p15 = new Product("Dron Plegable GPS", "Tomas aéreas", 345.80);
        assignCategories(p15, catMap, "Foto y Video");
        products.add(p15);

        Product p16 = new Product("Gafas Realidad Mixta", "El futuro hoy", 2400.00);
        assignCategories(p16, catMap, "Destacado", "Monitores y Pantallas");
        products.add(p16);

        Product p17 = new Product("Impresora 3D Resina", "Crea prototipos", 425.99);
        assignCategories(p17, catMap, "Herramientas y Montaje");
        products.add(p17);

        Product p30 = new Product("Barra de Sonido Atmos", "Cine en casa compacto", 350.00);
        p30.setPreviousPrice(450.00);
        assignCategories(p30, catMap, "Audio y Sonido");
        products.add(p30);

        for (Product p : products) {
            ProductImageInfo pImage = new ProductImageInfo(GlobalDefaults.PRODUCT_IMAGE, p);
            p.getImages().add(pImage);
            productRepository.save(p);
        }
    }

    private void assignCategories(Product product, Map<String, Category> catMap, String... categoryNames) {
        List<Category> categoriesToAssign = new ArrayList<>();
        for (String name : categoryNames) {
            Category c = catMap.get(name);
            if (c != null) {
                categoriesToAssign.add(c);
            } else {
                log.warn("Category not found in initialization: {}", name);
            }
        }
        product.setCategories(categoriesToAssign);
    }

    private void initShopsAndTrucks() {
        if (shopRepository.count() > 0) return;
        log.info(">>> Initializing Shops and Trucks...");

        Address address1 = new Address("Madrid-Recoletos", "CallePorDefecto4", "3", "", "28900", "Madrid", "España");
        Shop shop1 = new Shop("Madrid-Recoletos", address1, -3.7038, 40.4168);
        shop1.setImage(GlobalDefaults.SHOP_IMAGE);
        Optional<User> manager = userRepository.findByUsername("manager");
        if(manager.isPresent()){
            shop1.setAssignedManager(manager.get());
        }
        shopRepository.save(shop1);

        Address address2 = new Address("Alicante", "Calle Por Defecto", "43", "", "03002", "Alicante", "España");
        Shop shop2 = new Shop("Alicante", address2, -0.485225, 38.348045);
        shop2.setImage(GlobalDefaults.SHOP_IMAGE);
        shopRepository.save(shop2);

        Truck truck1 = truckRepository.save(new Truck("2C4RD", -3.6038, 40.6168));
        Truck truck2 = truckRepository.save(new Truck("5U7TH", -3.9038, 40.5168));
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
        List<Product> products = productRepository.findAllWithImages();

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
            cartItems.add(new OrderItem(products.get(0), user1, 1));
            cartItems.add(new OrderItem(products.get(2), user1, 2));
            cartItems.add(new OrderItem(products.get(3), user1, 3));
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
            // Use getContentAsByteArray() instead of getInputStream()
            byte[] bytes = resource.getContentAsByteArray();
            Map<String, String> result = storageService.uploadFile(
                    bytes,
                    resource.getFilename(),
                    "image/jpeg",
                    folder
            );
            return new ImageInfo(result.get("url"), result.get("key"), resource.getFilename());
        } catch (Exception e) {
            log.error("FATAL ERROR uploading default image {}: {}", resource.getFilename(), e.getMessage());
            throw new RuntimeException("CRITICAL: Error uploading default image: " + resource.getFilename(), e);
        }
    }


    private Map<String, String> uploadToMinio(ClassPathResource resource, String originalName, String folder) throws IOException {
        return storageService.uploadFile(
                resource.getContentAsByteArray(),
                originalName,
                "image/jpeg",
                folder
        );
    }
}