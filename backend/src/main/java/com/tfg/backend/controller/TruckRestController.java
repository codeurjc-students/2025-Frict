package com.tfg.backend.controller;

import com.tfg.backend.dto.PageResponse;
import com.tfg.backend.dto.TruckDTO;
import com.tfg.backend.model.Truck;
import com.tfg.backend.model.TruckStatus;
import com.tfg.backend.service.ShopTruckOrchestrator;
import com.tfg.backend.service.TruckService;
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

@RestController
@RequestMapping("/api/v1/trucks")
@Tag(name = "Trucks Management", description = "System trucks data management")
@RequiredArgsConstructor
public class TruckRestController {

    private final ShopTruckOrchestrator shopTruckOrchestrator;
    private final TruckService truckService;


    @Operation(summary = "(Admin) Get all trucks information (paged)")
    @GetMapping("/")
    public ResponseEntity<PageResponse<TruckDTO>> getAllTrucksPage(Pageable pageable) {
        Page<Truck> trucks = truckService.findAll(pageable);
        return ResponseEntity.ok(PageFormatter.toPageResponse(trucks, TruckDTO::new));
    }


    @Operation(summary = "(Admin, Manager) Get truck information by ID")
    @GetMapping("/{id}")
    public ResponseEntity<TruckDTO> getTruckById(@PathVariable Long id) {
        Truck truck = truckService.findTruckHelper(id);
        return ResponseEntity.ok(new TruckDTO(truck));
    }


    @Operation(summary = "(Driver) Get assigned truck by driver ID")
    @GetMapping("/user/{driverId}")
    public ResponseEntity<TruckDTO> getAssignedTruckByDriverId(@PathVariable Long driverId) {
        Truck assignedTruckByDriverId = truckService.getAssignedTruckByDriverId(driverId);
        if (assignedTruckByDriverId == null){
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(new TruckDTO(assignedTruckByDriverId));
    }


    @Operation(summary = "(Admin, Manager) Get trucks list by shop ID")
    @GetMapping("/shop/{shopId}/list")
    public ResponseEntity<List<TruckDTO>> getAllShopTrucks(@PathVariable Long shopId) {
        List<Truck> allShopTrucks = shopTruckOrchestrator.getAllShopTrucks(shopId);
        return ResponseEntity.ok(allShopTrucks.stream().map(TruckDTO::new).toList());
    }


    @Operation(summary = "(All) Get trucks page by shop ID")
    @GetMapping("/shop/{shopId}")
    public ResponseEntity<PageResponse<TruckDTO>> getTrucksByShopId(@PathVariable Long shopId, Pageable pageable) {
        Page<Truck> trucks = truckService.findAllByAssignedShopId(shopId, pageable);
        return ResponseEntity.ok(PageFormatter.toPageResponse(trucks, TruckDTO::new));
    }


    @Operation(summary = "(Admin, Manager) Get all unassigned trucks")
    @GetMapping("/available/")
    public ResponseEntity<List<TruckDTO>> getAllUnassignedTrucks() {
        List<Truck> unassignedTrucks = truckService.findAllByAssignedShopIsNull();
        return ResponseEntity.ok(unassignedTrucks.stream().map(TruckDTO::new).toList());
    }


    @Operation(summary = "(Admin) Set driver assignment to a truck")
    @PutMapping("/{truckId}/assign/driver/{driverId}")
    public ResponseEntity<TruckDTO> setAssignedDriver(@PathVariable Long driverId, @PathVariable Long truckId, @RequestParam boolean state) {
        Truck savedTruck = truckService.setAssignedDriver(driverId, truckId, state);
        return ResponseEntity.ok(new TruckDTO(savedTruck));
    }


    @Operation(summary = "(Admin) Comment and/or update truck status by ID")
    @PutMapping("/status/{id}")
    public ResponseEntity<TruckDTO> commentAndOrUpdateTruckStatus(@PathVariable Long id,
                                                                  @RequestParam TruckStatus truckStatus,
                                                                  @RequestParam(required = false) String comment){
        Truck savedTruck = truckService.commentAndOrUpdateTruckStatus(id, truckStatus, comment);
        return ResponseEntity.ok(new TruckDTO(savedTruck));
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

        return ResponseEntity.created(location).body(new TruckDTO(savedTruck));
    }

    @Operation(summary = "(Admin) Update truck by ID")
    @PutMapping("/{id}")
    public ResponseEntity<TruckDTO> updateTruck(@PathVariable Long id, @RequestBody TruckDTO truckDTO) {
        Truck updatedTruck = shopTruckOrchestrator.updateTruck(id, truckDTO);
        return ResponseEntity.accepted().body(new TruckDTO(updatedTruck));
    }


    @Operation(summary = "(Admin) Delete truck by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<TruckDTO> deleteTruck(@PathVariable Long id) {
        Truck deletedTruck = truckService.deleteTruck(id);
        return ResponseEntity.ok(new TruckDTO(deletedTruck));
    }
}
