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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
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
    public ResponseEntity<PageResponse<ShopDTO>> getAssignedShopsPage(HttpServletRequest request, Pageable pageable) {
        User loggedUser = findLoggedUserHelper(request);
        Page<Shop> assignedShops = shopService.findAllByAssignedManagerId(loggedUser.getId(), pageable);
        return ResponseEntity.ok(PageFormatter.toPageResponse(assignedShops, ShopDTO::new));
    }


    @Operation(summary = "(Admin) Get all shops information (paged)")
    @GetMapping("/")
    public ResponseEntity<PageResponse<ShopDTO>> getAllShopsPage(Pageable pageable) {
        Page<Shop> allShops = shopService.findAll(pageable);
        return ResponseEntity.ok(PageFormatter.toPageResponse(allShops, ShopDTO::new));
    }


    @Operation(summary = "(Manager) Get shop information by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ShopDTO> getShopById(@PathVariable Long id) {
        Shop shop = findShopHelper(id);
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

        Shop shop = new Shop(shopDTO.getName(), address, shopDTO.getLongitude(), shopDTO.getLatitude());
        Shop savedShop = shopService.save(shop);
        return ResponseEntity.accepted().body(new ShopDTO(savedShop));
    }

    @Operation(summary = "(Admin) Update shop by ID")
    @PutMapping("/{id}")
    public ResponseEntity<ShopDTO> updateShop(@PathVariable Long id, @RequestBody ShopDTO shopDTO) {
        Shop shop = findShopHelper(id);

        shop.setName(shopDTO.getName());
        AddressDTO dto = shopDTO.getAddress();
        shop.setAddress(new Address(dto.getAlias(), dto.getStreet(), dto.getNumber(), dto.getFloor(), dto.getPostalCode(), dto.getCity(), dto.getCountry()));
        shop.setLongitude(shopDTO.getLongitude());
        shop.setLatitude(shopDTO.getLatitude());
        Shop updatedShop = shopService.save(shop);

        return ResponseEntity.accepted().body(new ShopDTO(updatedShop));
    }


    @Operation(summary = "(Admin) Delete shop by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<ShopDTO> deleteShop(@PathVariable Long id) {
        Shop shop = findShopHelper(id);

        //Unlink trucks
        List<Truck> assignedTrucks = shop.getAssignedTrucks();
        for (Truck truck : assignedTrucks) {
            truck.setAssignedShop(null);
        }
        truckService.saveAll(assignedTrucks);

        shopService.delete(shop);
        //Delete shop image (if it is not the default photo)
        if (!shop.getImage().equals(GlobalDefaults.SHOP_IMAGE)) {
            storageService.deleteFile(shop.getImage().getS3Key());
        }

        return ResponseEntity.ok(new ShopDTO(shop));
    }


    @Operation(summary = "(Manager) Toggle stock local activation by ID")
    @PostMapping("/active/{id}")
    public ResponseEntity<ShopStockDTO> toggleLocalActivation(@PathVariable Long id, @RequestParam boolean state) {
        ShopStock stock = findShopStockHelper(id);
        stock.setActive(state);
        ShopStock savedStock = shopStockService.save(stock);
        return ResponseEntity.ok(new ShopStockDTO(savedStock));
    }


    @Operation(summary = "(Manager) Toggle all stocks local activation")
    @PostMapping("/{shopId}/active/")
    public ResponseEntity<Boolean> toggleAllLocalActivations(@PathVariable Long shopId, @RequestParam boolean state) {
        List<ShopStock> stocks = this.shopStockService.findAllByShopId(shopId);
        for (ShopStock s : stocks) {
            s.setActive(state);
        }
        shopStockService.saveAll(stocks);
        return ResponseEntity.ok(state); //State all toggles should have in frontend
    }


    @Operation(summary = "(Manager) Add n units to a shop's product stock (if exists)")
    @PostMapping("/restock/{stockId}")
    public ResponseEntity<ShopStockDTO> restockProduct(@PathVariable Long stockId, @RequestParam int units) {
        ShopStock targetStock = findShopStockHelper(stockId);
        if (units > 0){
            targetStock.setUnits(targetStock.getUnits() + units);
            ShopStock savedStock = shopStockService.save(targetStock);
            return ResponseEntity.ok(new ShopStockDTO(savedStock));
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Restock units must be positive, " + units + " is not a valid number.");
    }


    @Operation(summary = "(Manager) Set stock assignment to a shop")
    @PostMapping("/{shopId}/assign/stock/{stockId}")
    public ResponseEntity<ShopStockDTO> setAssignedStock(@PathVariable Long shopId, @PathVariable Long stockId, @RequestParam boolean state) {
        Shop shop = findShopHelper(shopId);
        ShopStock targetStock;

        //Add a stock: stockId is the product identifier
        if (state) {
            Product product = findProductHelper(stockId);
            targetStock = this.shopStockService.save(new ShopStock(shop, product, 0));
        }
        //Remove a stock: stockId is the stock identifier
        else {
            targetStock = findShopStockHelper(stockId);
            this.shopStockService.deleteById(stockId);
        }
        return ResponseEntity.ok(new ShopStockDTO(targetStock));
    }


    @Operation(summary = "(Manager) Set truck assignment to a shop")
    @PostMapping("/{shopId}/assign/truck/{truckId}")
    public ResponseEntity<TruckDTO> setAssignedTruck(@PathVariable Long shopId, @PathVariable Long truckId, @RequestParam boolean state) {
        Shop shop = findShopHelper(shopId);
        Truck truck = findTruckHelper(truckId);

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
    @PostMapping("/{shopId}/assign/manager/{userId}")
    public ResponseEntity<ShopDTO> setAssignedManager(@PathVariable Long shopId, @PathVariable Long userId, @RequestParam boolean state) {
        Shop shop = findShopHelper(shopId);

        if (state) {
            User newManager = findUserHelper(userId);
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
    @PostMapping("/image/{id}")
    public ResponseEntity<ShopDTO> uploadShopImage(@PathVariable Long id, @RequestParam("image") MultipartFile image) throws IOException {
        Shop shop = findShopHelper(id);

        // Clean previous image (if exists and it is not the default user image)
        if (shop.getImage() != null && !shop.getImage().equals(GlobalDefaults.SHOP_IMAGE)) {
            storageService.deleteFile(shop.getImage().getS3Key());
        }

        if (image.isEmpty()){
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
        Shop shop = findShopHelper(id);

        // Clean previous image (if exists and it is not the default user image)
        if (!shop.getImage().equals(GlobalDefaults.SHOP_IMAGE)) {
            storageService.deleteFile(shop.getImage().getS3Key());
            shop.setImage(GlobalDefaults.SHOP_IMAGE);
            return ResponseEntity.ok(new ShopDTO(shopService.save(shop)));
        }

        return ResponseEntity.ok(new ShopDTO(shop));
    }


    private User findLoggedUserHelper(HttpServletRequest request) {
        return this.userService.getLoggedUser(request)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You must be logged to perform this operation."));
    }

    private Product findProductHelper(Long id) {
        return this.productService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product with ID " + id + " does not exist."));
    }


    private User findUserHelper(Long id) {
        return this.userService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User with ID " + id + " does not exist."));
    }

    private Shop findShopHelper(Long id) {
        return this.shopService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop with ID " + id + " does not exist."));
    }

    private ShopStock findShopStockHelper(Long id) {
        return this.shopStockService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock with ID " + id + " does not exist."));
    }

    private Truck findTruckHelper(Long id) {
        return this.truckService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Truck with ID " + id + " does not exist."));
    }
}
