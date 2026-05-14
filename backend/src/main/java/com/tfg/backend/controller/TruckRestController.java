package com.tfg.backend.controller;

import com.tfg.backend.dto.PageResponse;
import com.tfg.backend.dto.TruckDTO;
import com.tfg.backend.dto.UserDTO;
import com.tfg.backend.model.Truck;
import com.tfg.backend.model.TruckStatus;
import com.tfg.backend.service.ShopTruckOrchestrator;
import com.tfg.backend.service.TruckService;
import com.tfg.backend.service.ConnectionService;
import com.tfg.backend.utils.PageFormatter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/trucks")
@Tag(name = "Trucks Management", description = "System trucks data management")
@RequiredArgsConstructor
public class TruckRestController {

    private final ShopTruckOrchestrator shopTruckOrchestrator;
    private final TruckService truckService;

    // Inject the user connection service for presence enrichment
    private final ConnectionService connectionService;

    @Operation(summary = "(Admin) Get all trucks information (paged)")
    @GetMapping("/")
    public ResponseEntity<PageResponse<TruckDTO>> getAllTrucksPage(Pageable pageable) {
        Page<Truck> trucks = truckService.findAll(pageable);
        return ResponseEntity.ok(toEnrichedPageResponse(trucks));
    }

    @Operation(summary = "(Admin, Manager) Get truck information by ID")
    @GetMapping("/{id}")
    public ResponseEntity<TruckDTO> getTruckById(@PathVariable Long id) {
        Truck truck = truckService.findTruckHelper(id);
        return ResponseEntity.ok(toEnrichedDTO(truck));
    }

    @Operation(summary = "(Driver) Get assigned truck by driver ID")
    @GetMapping("/user/{driverId}")
    public ResponseEntity<TruckDTO> getAssignedTruckByDriverId(@PathVariable Long driverId) {
        Truck assignedTruckByDriverId = truckService.getAssignedTruckByDriverId(driverId);
        if (assignedTruckByDriverId == null){
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(toEnrichedDTO(assignedTruckByDriverId));
    }

    @Operation(summary = "(Admin, Manager) Get trucks list by shop ID")
    @GetMapping("/shop/{shopId}/list")
    public ResponseEntity<List<TruckDTO>> getAllShopTrucks(@PathVariable Long shopId) {
        List<Truck> allShopTrucks = shopTruckOrchestrator.getAllShopTrucks(shopId);
        return ResponseEntity.ok(toEnrichedDTOList(allShopTrucks));
    }

    @Operation(summary = "(All) Get trucks page by shop ID")
    @GetMapping("/shop/{shopId}")
    public ResponseEntity<PageResponse<TruckDTO>> getTrucksByShopId(@PathVariable Long shopId, Pageable pageable) {
        Page<Truck> trucks = truckService.findAllByAssignedShopId(shopId, pageable);
        return ResponseEntity.ok(toEnrichedPageResponse(trucks));
    }

    @Operation(summary = "(Admin, Manager) Get all unassigned trucks")
    @GetMapping("/available/")
    public ResponseEntity<List<TruckDTO>> getAllUnassignedTrucks() {
        List<Truck> unassignedTrucks = truckService.findAllByAssignedShopIsNull();
        return ResponseEntity.ok(toEnrichedDTOList(unassignedTrucks));
    }

    @Operation(summary = "(Admin) Set driver assignment to a truck")
    @PutMapping("/{truckId}/assign/driver/{driverId}")
    public ResponseEntity<TruckDTO> setAssignedDriver(@PathVariable Long driverId, @PathVariable Long truckId, @RequestParam boolean state) {
        Truck savedTruck = truckService.setAssignedDriver(driverId, truckId, state);
        return ResponseEntity.ok(toEnrichedDTO(savedTruck));
    }

    @Operation(summary = "(Admin) Comment and/or update truck status by ID")
    @PutMapping("/status/{id}")
    public ResponseEntity<TruckDTO> commentAndOrUpdateTruckStatus(@PathVariable Long id,
                                                                  @RequestParam TruckStatus truckStatus,
                                                                  @RequestParam(required = false) String comment){
        Truck savedTruck = truckService.commentAndOrUpdateTruckStatus(id, truckStatus, comment);
        return ResponseEntity.ok(toEnrichedDTO(savedTruck));
    }

    @Operation(summary = "(Admin) Create truck")
    @PostMapping
    public ResponseEntity<TruckDTO> createTruck(@RequestBody TruckDTO truckDTO) {
        Truck savedTruck = shopTruckOrchestrator.createTruck(truckDTO);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedTruck.getId())
                .toUri();

        return ResponseEntity.created(location).body(toEnrichedDTO(savedTruck));
    }

    @Operation(summary = "(Admin) Update truck by ID")
    @PutMapping("/{id}")
    public ResponseEntity<TruckDTO> updateTruck(@PathVariable Long id, @RequestBody TruckDTO truckDTO) {
        Truck updatedTruck = shopTruckOrchestrator.updateTruck(id, truckDTO);
        return ResponseEntity.accepted().body(toEnrichedDTO(updatedTruck));
    }

    @Operation(summary = "(Admin) Delete truck by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<TruckDTO> deleteTruck(@PathVariable Long id) {
        Truck deletedTruck = truckService.deleteTruck(id);
        return ResponseEntity.ok(toEnrichedDTO(deletedTruck));
    }

    // ==========================================
    // ASSEMBLER AND ENRICHMENT HELPER METHODS
    // ==========================================

    /**
     * Converts a Truck entity to a DTO and enriches its driver (if assigned).
     */
    private TruckDTO toEnrichedDTO(Truck truck) {
        TruckDTO dto = truckService.toTruckDTO(truck);
        if (dto.getAssignedDriver() != null) {
            connectionService.enrichWithConnection(dto.getAssignedDriver());
        }
        return dto;
    }

    /**
     * Converts a list of Truck entities to DTOs and enriches all drivers in batch.
     */
    private List<TruckDTO> toEnrichedDTOList(List<Truck> trucks) {
        List<TruckDTO> dtos = trucks.stream().map(truckService::toTruckDTO).toList();

        List<UserDTO> drivers = dtos.stream()
                .map(TruckDTO::getAssignedDriver)
                .filter(Objects::nonNull)
                .toList();

        connectionService.enrichWithConnections(drivers);
        return dtos;
    }

    /**
     * Converts a page of Truck entities to a paginated response of enriched DTOs.
     */
    private PageResponse<TruckDTO> toEnrichedPageResponse(Page<Truck> trucks) {
        // 1. Map the pure entity page to a DTO page
        Page<TruckDTO> dtoPage = trucks.map(truckService::toTruckDTO);

        // 2. Extract drivers from the current page and enrich them in a single batch query
        List<UserDTO> drivers = dtoPage.getContent().stream()
                .map(TruckDTO::getAssignedDriver)
                .filter(Objects::nonNull)
                .toList();

        connectionService.enrichWithConnections(drivers);

        // 3. Return the formatted PageResponse (identity function "dto -> dto" as they are already mapped)
        return PageFormatter.toPageResponse(dtoPage, dto -> dto);
    }
}