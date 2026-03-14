package com.tfg.backend.controller;

import com.tfg.backend.dto.*;
import com.tfg.backend.model.*;
import com.tfg.backend.service.*;
import com.tfg.backend.utils.GlobalDefaults;
import com.tfg.backend.utils.PageFormatter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.*;

@RestController
@RequestMapping("/api/v1/shops")
@Tag(name = "Shop Management", description = "Shop data management")
public class ShopRestController {

    @Autowired
    private ShopService shopService;

    @Autowired
    private TruckService truckService;

    @Autowired
    private StorageService storageService;

    @Autowired
    private UserService userService;

    @Autowired
    private ShopStockService shopStockService;

    @Autowired
    private ProductService productService;


    @Operation(summary = "(Manager) Get assigned shops information (paged)")
    @GetMapping
    public ResponseEntity<PageResponse<ShopDTO>> getAssignedShopsPage(Pageable pageable) {
        User loggedUser = userService.findLoggedUserHelper();
        Page<Shop> assignedShops = shopService.findAllByAssignedManagerId(loggedUser.getId(), pageable);
        return ResponseEntity.ok(PageFormatter.toPageResponse(assignedShops, ShopDTO::new));
    }


    @Operation(summary = "(Admin) Get all shops information (paged)")
    @GetMapping("/")
    public ResponseEntity<PageResponse<ShopDTO>> getAllShopsPage(Pageable pageable) {
        Page<Shop> allShops = shopService.findAll(pageable);
        return ResponseEntity.ok(PageFormatter.toPageResponse(allShops, ShopDTO::new));
    }


    @Operation(summary = "(User) Get all available shops to be selected")
    @GetMapping("/list")
    public ResponseEntity<List<ShopDTO>> getAllShopsList() {
        List<Shop> allShops = shopService.findAll();
        List<ShopDTO> dtos = new ArrayList<>();
        for (Shop s : allShops) {
            dtos.add(new ShopDTO(s));
        }
        return ResponseEntity.ok(dtos);
    }


    @Operation(summary = "(Manager) Get shop information by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ShopDTO> getShopById(@PathVariable Long id) {
        Shop shop = shopService.findShopHelper(id);
        return ResponseEntity.ok(new ShopDTO(shop));
    }


    @Operation(summary = "(All) Get shop stock page by ID")
    @GetMapping("/stock/{id}")
    public ResponseEntity<PageResponse<ShopStockDTO>> getShopStocks(@PathVariable Long id, Pageable pageable) {
        Page<ShopStock> stocks = shopStockService.findAllByShopId(id, pageable);
        return ResponseEntity.ok(PageFormatter.toPageResponse(stocks, ShopStockDTO::new));
    }


    @Operation(summary = "(Admin) Create shop")
    @PostMapping
    public ResponseEntity<ShopDTO> createShop(@RequestBody ShopDTO shopDTO) {
        AddressDTO dto = shopDTO.getAddress();
        Address address = new Address(dto.getAlias(), dto.getStreet(), dto.getNumber(), dto.getFloor(), dto.getPostalCode(), dto.getCity(), dto.getCountry());
        address.setLatitude(shopDTO.getAddress().getLatitude());
        address.setLongitude(shopDTO.getAddress().getLongitude());
        Shop shop = new Shop(shopDTO.getName(), address, shopDTO.getAssignedBudget());
        Shop savedShop = shopService.save(shop);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedShop.getId())
                .toUri();

        return ResponseEntity.created(location).body(new ShopDTO(savedShop));
    }

    @Operation(summary = "(Admin) Update shop by ID")
    @PutMapping("/{id}")
    public ResponseEntity<ShopDTO> updateShop(@PathVariable Long id, @RequestBody ShopDTO shopDTO) {
        Shop shop = shopService.findShopHelper(id);

        shop.setName(shopDTO.getName());
        AddressDTO dto = shopDTO.getAddress();
        Address address = new Address(dto.getAlias(), dto.getStreet(), dto.getNumber(), dto.getFloor(), dto.getPostalCode(), dto.getCity(), dto.getCountry());
        address.setLatitude(dto.getLatitude());
        address.setLongitude(dto.getLongitude());
        shop.setAddress(address);
        shop.setAssignedBudget(shopDTO.getAssignedBudget());
        Shop updatedShop = shopService.save(shop);

        return ResponseEntity.accepted().body(new ShopDTO(updatedShop));
    }


    @Transactional // Imprescindible para que Hibernate gestione todos los updates y el delete juntos
    @Operation(summary = "(Admin) Delete shop by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<ShopDTO> deleteShop(@PathVariable Long id) {
        Shop shop = shopService.findShopHelper(id);

        // Unlink trucks
        List<Truck> assignedTrucks = new ArrayList<>(shop.getAssignedTrucks());
        for (Truck truck : assignedTrucks) {
            truck.setAssignedShop(null);
        }
        shop.getAssignedTrucks().clear();

        // Unlink orders
        List<Order> assignedOrders = new ArrayList<>(shop.getAssignedOrders());
        for (Order order : assignedOrders) {
            order.setAssignedShop(null);

            if(order.getCurrentStatus() == OrderStatus.ORDER_MADE || order.getCurrentStatus() == OrderStatus.SENT){
                order.changeOrderStatus(OrderStatus.CANCELLED, "La tienda a la que estaba asignado el pedido ha sido eliminada.");
            }
        }
        shop.getAssignedOrders().clear();

        // Unlink clients (that have this shop as selected)
        List<User> customers = new ArrayList<>(shop.getCustomers());
        for (User customer : customers) {
            customer.setSelectedShop(null);
        }
        shop.getCustomers().clear();

        shopService.delete(shop);

        // Delete shop image if it is not the default image
        if (shop.getImage() != null && !shop.getImage().equals(GlobalDefaults.SHOP_IMAGE)) {
            storageService.deleteFile(shop.getImage().getS3Key());
        }

        return ResponseEntity.ok(new ShopDTO(shop));
    }


    @Operation(summary = "(Manager) Toggle stock local activation by ID")
    @PutMapping("/active/{id}")
    public ResponseEntity<ShopStockDTO> toggleLocalActivation(@PathVariable Long id, @RequestParam boolean state) {
        ShopStock stock = shopStockService.findShopStockHelper(id);
        stock.setActive(state);
        ShopStock savedStock = shopStockService.save(stock);
        return ResponseEntity.ok(new ShopStockDTO(savedStock));
    }


    @Operation(summary = "(Manager) Toggle all stocks local activation")
    @PutMapping("/{shopId}/active/")
    public ResponseEntity<Boolean> toggleAllLocalActivations(@PathVariable Long shopId, @RequestParam boolean state) {
        List<ShopStock> stocks = this.shopStockService.findAllByShopId(shopId);
        for (ShopStock s : stocks) {
            s.setActive(state);
        }
        shopStockService.saveAll(stocks);
        return ResponseEntity.ok(state); //State all toggles should have in frontend
    }


    @Operation(summary = "(Manager) Add n units to a shop's product stock (if exists)")
    @PutMapping("/restock/{stockId}")
    public ResponseEntity<ShopStockDTO> restockProduct(@PathVariable Long stockId, @RequestParam int units) {
        ShopStock targetStock = shopStockService.findShopStockHelper(stockId);

        if (units <= 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Restock units must be positive, " + units + " is not a valid number.");
        }

        Shop restockingShop = targetStock.getShop();
        double supplyCost = units * targetStock.getProduct().getSupplyPrice();
        if (supplyCost > restockingShop.getAssignedBudget()){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "There is not enough budget in this shop to complete this operation.");
        }

        targetStock.setUnits(targetStock.getUnits() + units);
        restockingShop.setAssignedBudget(restockingShop.getAssignedBudget() - supplyCost);
        shopService.save(restockingShop);
        ShopStock savedStock = shopStockService.save(targetStock);
        return ResponseEntity.ok(new ShopStockDTO(savedStock));
    }


    @Operation(summary = "(Manager) Set stock assignment to a shop")
    @PutMapping("/{shopId}/assign/stock/{stockId}")
    public ResponseEntity<ShopStockDTO> setAssignedStock(@PathVariable Long shopId, @PathVariable Long stockId, @RequestParam boolean state) {
        Shop shop = shopService.findShopHelper(shopId);
        ShopStock targetStock;

        //Add a stock: stockId is the product identifier
        if (state) {
            Product product = productService.findProductHelper(stockId);
            targetStock = this.shopStockService.save(new ShopStock(shop, product, 0));
        }
        //Remove a stock: stockId is the stock identifier
        else {
            targetStock = shopStockService.findShopStockHelper(stockId);
            this.shopStockService.deleteById(stockId);
        }
        return ResponseEntity.ok(new ShopStockDTO(targetStock));
    }


    @Operation(summary = "(Manager) Set truck assignment to a shop")
    @PutMapping("/{shopId}/assign/truck/{truckId}")
    public ResponseEntity<TruckDTO> setAssignedTruck(@PathVariable Long shopId, @PathVariable Long truckId, @RequestParam boolean state) {
        Shop shop = shopService.findShopHelper(shopId);
        Truck truck = truckService.findTruckHelper(truckId);

        if (state) {
            truck.setAssignedShop(shop);
        }
        else {
            truck.setAssignedShop(null);
        }

        Truck savedTruck = truckService.save(truck);
        return ResponseEntity.ok(new TruckDTO(savedTruck));
    }


    @Operation(summary = "(Admin) Set manager assignment to a shop")
    @PutMapping("/{shopId}/assign/manager/{userId}")
    public ResponseEntity<ShopDTO> setAssignedManager(@PathVariable Long shopId, @PathVariable Long userId, @RequestParam boolean state) {
        Shop shop = shopService.findShopHelper(shopId);

        if (state) {
            User newManager = userService.findUserHelper(userId);
            shop.setAssignedManager(newManager);
        }
        else {
            User currentManager = shop.getAssignedManager();
            if (currentManager != null && currentManager.getId().equals(userId)) {
                shop.setAssignedManager(null);
            }
        }

        Shop savedShop = shopService.save(shop);
        return ResponseEntity.ok(new ShopDTO(savedShop));
    }


    @Operation(summary = "(Admin) Update remote shop image")
    @PutMapping("/image/{id}")
    public ResponseEntity<ShopDTO> uploadShopImage(@PathVariable Long id, @RequestParam(value = "image", required = false) MultipartFile image) throws IOException {
        Shop shop = shopService.findShopHelper(id);

        // Clean previous image (if exists and it is not the default user image)
        if (shop.getImage() != null && !shop.getImage().equals(GlobalDefaults.SHOP_IMAGE)) {
            storageService.deleteFile(shop.getImage().getS3Key());
        }

        if (image != null && !image.isEmpty()){
            Map<String, String> res = storageService.uploadFile(image, "shops");
            ImageInfo shopImageInfo = new ImageInfo(
                    res.get("url"),
                    res.get("key"),
                    image.getOriginalFilename()
            );
            shop.setImage(shopImageInfo);
        }
        else {
            shop.setImage(GlobalDefaults.SHOP_IMAGE);
        }

        return ResponseEntity.ok(new ShopDTO(shopService.save(shop)));
    }


    @Operation(summary = "(Admin) Delete remote shop image")
    @DeleteMapping("/image/{id}")
    public ResponseEntity<ShopDTO> deleteShopImage(@PathVariable Long id) {
        Shop shop = shopService.findShopHelper(id);

        // Clean previous image (if exists and it is not the default user image)
        if (!shop.getImage().equals(GlobalDefaults.SHOP_IMAGE)) {
            storageService.deleteFile(shop.getImage().getS3Key());
            shop.setImage(GlobalDefaults.SHOP_IMAGE);
            return ResponseEntity.ok(new ShopDTO(shopService.save(shop)));
        }

        return ResponseEntity.ok(new ShopDTO(shop));
    }
}
