package com.tfg.backend.controller;

import com.tfg.backend.dto.PageResponse;
import com.tfg.backend.dto.ShopDTO;
import com.tfg.backend.dto.ShopStockDTO;
import com.tfg.backend.dto.TruckDTO;
import com.tfg.backend.dto.UserDTO;
import com.tfg.backend.model.Shop;
import com.tfg.backend.model.ShopStock;
import com.tfg.backend.model.Truck;
import com.tfg.backend.notification.UserConnectionService;
import com.tfg.backend.service.ShopService;
import com.tfg.backend.service.ShopStockService;
import com.tfg.backend.service.ShopTruckOrchestrator;
import com.tfg.backend.service.ShopUserOrchestrator;
import com.tfg.backend.utils.PageFormatter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/shops")
@Tag(name = "Shop Management", description = "Shop data management")
@RequiredArgsConstructor
public class ShopRestController {

    private final ShopTruckOrchestrator shopTruckOrchestrator;
    private final ShopService shopService;
    private final ShopStockService shopStockService;
    private final ShopUserOrchestrator shopUserOrchestrator;

    // Inject the user connection service for presence enrichment
    private final UserConnectionService userConnectionService;

    @Operation(summary = "(Manager) Get lightweight list of managed shops (Key-Value)")
    @GetMapping("/references")
    public ResponseEntity<List<Map<String, Object>>> getManagedShopReferences() {
        return ResponseEntity.ok(shopUserOrchestrator.getManagedShopReferences());
    }

    @Operation(summary = "(Manager) Get assigned shops information (paged)")
    @GetMapping
    public ResponseEntity<PageResponse<ShopDTO>> getAssignedShopsPage(Pageable pageable) {
        Page<Shop> assignedShopsPage = shopUserOrchestrator.getAssignedShopsPage(pageable);
        return ResponseEntity.ok(toEnrichedPageResponse(assignedShopsPage));
    }

    @Operation(summary = "(Admin) Get all shops information (paged)")
    @GetMapping("/")
    public ResponseEntity<PageResponse<ShopDTO>> getAllShopsPage(Pageable pageable) {
        Page<Shop> allShops = shopService.findAll(pageable);
        return ResponseEntity.ok(toEnrichedPageResponse(allShops));
    }

    @Operation(summary = "(User) Get all available shops to be selected")
    @GetMapping("/list")
    public ResponseEntity<List<ShopDTO>> getAllShopsList() {
        List<Shop> allShops = shopService.findAll();
        return ResponseEntity.ok(toEnrichedDTOList(allShops));
    }

    @Operation(summary = "(Manager) Get shop information by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ShopDTO> getShopById(@PathVariable Long id) {
        Shop shop = shopService.findShopHelper(id);
        return ResponseEntity.ok(toEnrichedDTO(shop));
    }

    @Operation(summary = "(Driver) Get shop information by assigned truck ID")
    @GetMapping("/truck/{id}")
    public ResponseEntity<ShopDTO> getShopByAssignedTruckId(@PathVariable Long id) {
        Shop shopByAssignedTruckId = shopTruckOrchestrator.getShopByAssignedTruckId(id);
        return ResponseEntity.ok(toEnrichedDTO(shopByAssignedTruckId));
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
        Shop savedShop = shopUserOrchestrator.createShop(shopDTO);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedShop.getId())
                .toUri();

        return ResponseEntity.created(location).body(toEnrichedDTO(savedShop));
    }

    @Operation(summary = "(Admin, Manager) Update shop by ID")
    @PutMapping("/{id}")
    public ResponseEntity<ShopDTO> updateShop(@PathVariable Long id, @RequestBody ShopDTO shopDTO) {
        Shop updatedShop = shopUserOrchestrator.updateShop(id, shopDTO);
        return ResponseEntity.accepted().body(toEnrichedDTO(updatedShop));
    }

    @Transactional
    @Operation(summary = "(Admin) Delete shop by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<ShopDTO> deleteShop(@PathVariable Long id) {
        Shop deletedShop = shopUserOrchestrator.deleteShop(id);
        return ResponseEntity.ok(toEnrichedDTO(deletedShop));
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
        return ResponseEntity.ok(savedState); // State all toggles should have in frontend
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
        Truck savedTruck = shopTruckOrchestrator.setAssignedTruck(shopId, truckId, state);
        // Uses specific Truck helper to ensure driver connection state is enriched
        return ResponseEntity.ok(toEnrichedTruckDTO(savedTruck));
    }

    @Operation(summary = "(Admin) Set manager assignment to a shop")
    @PutMapping("/{shopId}/assign/manager/{userId}")
    public ResponseEntity<ShopDTO> setAssignedManager(@PathVariable Long shopId, @PathVariable Long userId, @RequestParam boolean state) {
        Shop savedShop = shopUserOrchestrator.setAssignedManager(shopId, userId, state);
        return ResponseEntity.ok(toEnrichedDTO(savedShop));
    }

    @Operation(summary = "(Admin, Manager) Update remote shop image")
    @PutMapping("/image/{id}")
    public ResponseEntity<ShopDTO> uploadShopImage(@PathVariable Long id, @RequestParam(value = "image", required = false) MultipartFile image) throws IOException {
        Shop shop = shopService.uploadShopImage(id, image);
        return ResponseEntity.ok(toEnrichedDTO(shop));
    }

    @Operation(summary = "(Admin) Delete remote shop image")
    @DeleteMapping("/image/{id}")
    public ResponseEntity<ShopDTO> deleteShopImage(@PathVariable Long id) {
        Shop shop = shopService.deleteShopImage(id);
        return ResponseEntity.ok(toEnrichedDTO(shop));
    }

    // ==========================================
    // ASSEMBLER AND ENRICHMENT HELPER METHODS
    // ==========================================

    /**
     * Converts a Shop entity to a DTO and enriches its manager (if assigned).
     */
    private ShopDTO toEnrichedDTO(Shop shop) {
        ShopDTO dto = new ShopDTO(shop);
        if (dto.getAssignedManager() != null) {
            userConnectionService.enrichWithConnection(dto.getAssignedManager());
        }
        return dto;
    }

    /**
     * Converts a list of Shop entities to DTOs and enriches all managers in batch.
     */
    private List<ShopDTO> toEnrichedDTOList(List<Shop> shops) {
        List<ShopDTO> dtos = shops.stream().map(ShopDTO::new).toList();

        List<UserDTO> managers = dtos.stream()
                .map(ShopDTO::getAssignedManager)
                .filter(Objects::nonNull)
                .toList();

        userConnectionService.enrichWithConnections(managers);
        return dtos;
    }

    /**
     * Converts a page of Shop entities to a paginated response of enriched DTOs.
     */
    private PageResponse<ShopDTO> toEnrichedPageResponse(Page<Shop> shops) {
        // 1. Map the pure entity page to a DTO page
        Page<ShopDTO> dtoPage = shops.map(ShopDTO::new);

        // 2. Extract managers from the current page and enrich them in a single batch query
        List<UserDTO> managers = dtoPage.getContent().stream()
                .map(ShopDTO::getAssignedManager)
                .filter(Objects::nonNull)
                .toList();

        userConnectionService.enrichWithConnections(managers);

        // 3. Return the formatted PageResponse
        return PageFormatter.toPageResponse(dtoPage, dto -> dto);
    }

    /**
     * Converts a Truck entity to a DTO and enriches its driver (if assigned).
     * Used specifically for the setAssignedTruck endpoint.
     */
    private TruckDTO toEnrichedTruckDTO(Truck truck) {
        TruckDTO dto = new TruckDTO(truck);
        if (dto.getAssignedDriver() != null) {
            userConnectionService.enrichWithConnection(dto.getAssignedDriver());
        }
        return dto;
    }
}