package com.tfg.backend.controller;

import com.tfg.backend.dto.*;
import com.tfg.backend.model.*;
import com.tfg.backend.service.ShopService;
import com.tfg.backend.service.StorageService;
import com.tfg.backend.service.TruckService;
import com.tfg.backend.utils.GlobalDefaults;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    @Operation(summary = "Get all shops information (paged)")
    @GetMapping("/")
    public ResponseEntity<PageResponse<ShopDTO>> getShopsPage(Pageable pageable) {
        Page<Shop> allShops = shopService.findAll(pageable);
        return ResponseEntity.ok(toPageResponse(allShops));
    }


    @Operation(summary = "Get shop information by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ShopDTO> getShopById(@PathVariable Long id) {
        Shop shop = findShopHelper(id);
        return ResponseEntity.ok(new ShopDTO(shop));
    }

    @Operation(summary = "Create shop")
    @PostMapping
    public ResponseEntity<ShopDTO> createShop(@RequestBody ShopDTO shopDTO) {
        AddressDTO dto = shopDTO.getAddress();
        Address address = new Address(dto.getAlias(), dto.getStreet(), dto.getNumber(), dto.getFloor(), dto.getPostalCode(), dto.getCity(), dto.getCountry());

        Shop shop = new Shop(shopDTO.getName(), address, shopDTO.getLongitude(), shopDTO.getLatitude());
        Shop savedShop = shopService.save(shop);
        return ResponseEntity.accepted().body(new ShopDTO(savedShop));
    }

    @Operation(summary = "Update shop by ID")
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


    @Operation(summary = "Delete shop by ID")
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


    @Operation(summary = "Update remote shop image")
    @PostMapping("/image/{id}")
    public ResponseEntity<ShopDTO> uploadShopImage(@PathVariable Long id, @RequestParam("image") MultipartFile image) throws IOException {
        Shop shop = findShopHelper(id);

        // Clean previous image (if exists and it is not the default user image)
        if (shop.getImage() != null && !shop.getImage().equals(GlobalDefaults.SHOP_IMAGE)) {
            storageService.deleteFile(shop.getImage().getS3Key());
        }

        // Upload
        Map<String, String> res = storageService.uploadFile(image, "shops");

        // Create ImageInfo object (not an entity)
        ImageInfo userImageInfo = new ImageInfo(
                res.get("url"),
                res.get("key"),
                image.getOriginalFilename()
        );

        // Add to user
        shop.setImage(userImageInfo);

        return ResponseEntity.ok(new ShopDTO(shopService.save(shop)));
    }


    @Operation(summary = "Delete remote shop image")
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


    private PageResponse<ShopDTO> toPageResponse(Page<Shop> shops){
        List<ShopDTO> dtos = new ArrayList<>();
        for (Shop s : shops.getContent()) {
            ShopDTO dto = new ShopDTO(s);
            dtos.add(dto);
        }
        return new PageResponse<>(dtos, shops.getTotalElements(), shops.getNumber(), shops.getTotalPages()-1, shops.getSize());
    }

    private Shop findShopHelper(Long id) {
        return this.shopService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop with ID " + id + " does not exist."));
    }
}
