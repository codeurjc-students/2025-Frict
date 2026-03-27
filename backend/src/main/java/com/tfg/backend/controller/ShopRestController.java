package com.tfg.backend.controller;

import com.tfg.backend.dto.PageResponse;
import com.tfg.backend.dto.ShopDTO;
import com.tfg.backend.dto.ShopStockDTO;
import com.tfg.backend.dto.TruckDTO;
import com.tfg.backend.model.Shop;
import com.tfg.backend.model.ShopStock;
import com.tfg.backend.model.Truck;
import com.tfg.backend.service.ShopService;
import com.tfg.backend.service.ShopStockService;
import com.tfg.backend.service.ShopTruckOrchestrator;
import com.tfg.backend.utils.PageFormatter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/shops")
@Tag(name = "Shop Management", description = "Shop data management")
public class ShopRestController {

    @Autowired
    private ShopTruckOrchestrator orchestrator;

    @Autowired
    private ShopService shopService;

    @Autowired
    private ShopStockService shopStockService;


    @Operation(summary = "(Manager) Get assigned shops information (paged)")
    @GetMapping
    public ResponseEntity<PageResponse<ShopDTO>> getAssignedShopsPage(Pageable pageable) {
        Page<Shop> assignedShopsPage = shopService.getAssignedShopsPage(pageable);
        return ResponseEntity.ok(PageFormatter.toPageResponse(assignedShopsPage, ShopDTO::new));
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
        List<ShopDTO> allShops = shopService.findAll().stream().map(ShopDTO::new).toList();
        return ResponseEntity.ok(allShops);
    }


    @Operation(summary = "(Manager) Get shop information by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ShopDTO> getShopById(@PathVariable Long id) {
        Shop shop = shopService.findShopHelper(id);
        return ResponseEntity.ok(new ShopDTO(shop));
    }


    @Operation(summary = "(Driver) Get shop information by assigned truck ID")
    @GetMapping("/truck/{id}")
    public ResponseEntity<ShopDTO> getShopByAssignedTruckId(@PathVariable Long id) {
        Shop shopByAssignedTruckId = orchestrator.getShopByAssignedTruckId(id);
        return ResponseEntity.ok(new ShopDTO(shopByAssignedTruckId));
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
        Shop savedShop = shopService.createShop(shopDTO);

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
        Shop updatedShop = shopService.updateShop(id, shopDTO);
        return ResponseEntity.accepted().body(new ShopDTO(updatedShop));
    }


    @Transactional
    @Operation(summary = "(Admin) Delete shop by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<ShopDTO> deleteShop(@PathVariable Long id) {
        Shop deletedShop = shopService.deleteShop(id);
        return ResponseEntity.ok(new ShopDTO(deletedShop));
    }


    @Operation(summary = "(Manager) Toggle stock local activation by ID")
    @PutMapping("/active/{id}")
    public ResponseEntity<ShopStockDTO> toggleLocalActivation(@PathVariable Long id, @RequestParam boolean state) {
        ShopStock savedStock = shopService.toggleLocalActivation(id, state);
        return ResponseEntity.ok(new ShopStockDTO(savedStock));
    }


    @Operation(summary = "(Manager) Toggle all stocks local activation")
    @PutMapping("/{shopId}/active/")
    public ResponseEntity<Boolean> toggleAllLocalActivations(@PathVariable Long shopId, @RequestParam boolean state) {
        boolean savedState = shopService.toggleAllLocalActivations(shopId, state);
        return ResponseEntity.ok(savedState); //State all toggles should have in frontend
    }


    @Operation(summary = "(Manager) Add n units to a shop's product stock (if exists)")
    @PutMapping("/restock/{stockId}")
    public ResponseEntity<ShopStockDTO> restockProduct(@PathVariable Long stockId, @RequestParam int units) {
        ShopStock restockedStock = shopService.restockProduct(stockId, units);
        return ResponseEntity.ok(new ShopStockDTO(restockedStock));
    }


    @Operation(summary = "(Manager) Set stock assignment to a shop")
    @PutMapping("/{shopId}/assign/stock/{stockId}")
    public ResponseEntity<ShopStockDTO> setAssignedStock(@PathVariable Long shopId, @PathVariable Long stockId, @RequestParam boolean state) {
        ShopStock targetStock = shopService.setAssignedStock(shopId, stockId, state);
        return ResponseEntity.ok(new ShopStockDTO(targetStock));
    }


    @Operation(summary = "(Manager) Set truck assignment to a shop")
    @PutMapping("/{shopId}/assign/truck/{truckId}")
    public ResponseEntity<TruckDTO> setAssignedTruck(@PathVariable Long shopId, @PathVariable Long truckId, @RequestParam boolean state) {
        Truck savedTruck = orchestrator.setAssignedTruck(shopId, truckId, state);
        return ResponseEntity.ok(new TruckDTO(savedTruck));
    }


    @Operation(summary = "(Admin) Set manager assignment to a shop")
    @PutMapping("/{shopId}/assign/manager/{userId}")
    public ResponseEntity<ShopDTO> setAssignedManager(@PathVariable Long shopId, @PathVariable Long userId, @RequestParam boolean state) {
        Shop savedShop = shopService.setAssignedManager(shopId, userId, state);
        return ResponseEntity.ok(new ShopDTO(savedShop));
    }


    @Operation(summary = "(Admin) Update remote shop image")
    @PutMapping("/image/{id}")
    public ResponseEntity<ShopDTO> uploadShopImage(@PathVariable Long id, @RequestParam(value = "image", required = false) MultipartFile image) throws IOException {
        Shop shop = shopService.uploadShopImage(id, image);
        return ResponseEntity.ok(new ShopDTO(shop));
    }


    @Operation(summary = "(Admin) Delete remote shop image")
    @DeleteMapping("/image/{id}")
    public ResponseEntity<ShopDTO> deleteShopImage(@PathVariable Long id) {
        Shop shop = shopService.deleteShopImage(id);
        return ResponseEntity.ok(new ShopDTO(shop));
    }
}
