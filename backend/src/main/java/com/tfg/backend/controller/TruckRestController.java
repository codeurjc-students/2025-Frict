package com.tfg.backend.controller;

import com.tfg.backend.dto.PageResponse;
import com.tfg.backend.dto.ShopStockDTO;
import com.tfg.backend.dto.TruckDTO;
import com.tfg.backend.model.Shop;
import com.tfg.backend.model.ShopStock;
import com.tfg.backend.model.Truck;
import com.tfg.backend.service.ShopService;
import com.tfg.backend.service.TruckService;
import com.tfg.backend.utils.PageFormatter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/trucks")
@Tag(name = "Trucks Management", description = "System trucks data management")
public class TruckRestController {

    @Autowired
    private ShopService shopService;

    @Autowired
    private TruckService truckService;


    @Operation(summary = "(Admin) Get all trucks information (paged)")
    @GetMapping("/")
    public ResponseEntity<PageResponse<TruckDTO>> getAllTrucksPage(Pageable pageable) {
        Page<Truck> trucks = truckService.findAll(pageable);
        return ResponseEntity.ok(PageFormatter.toPageResponse(trucks, TruckDTO::new));
    }


    @Operation(summary = "(Admin, Manager) Get truck information by ID")
    @GetMapping("/{id}")
    public ResponseEntity<TruckDTO> getTruckById(@PathVariable Long id) {
        Optional<Truck> truckOptional = truckService.findById(id);
        if(truckOptional.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Truck with ID " + id + " does not exist.");
        }
        return ResponseEntity.ok(new TruckDTO(truckOptional.get()));
    }


    @Operation(summary = "(Admin, Manager) Get trucks list by shop ID")
    @GetMapping("/shop/{shopId}/list")
    public ResponseEntity<List<TruckDTO>> getAllShopTrucks(@PathVariable Long shopId) {
        Shop shop = shopService.findShopHelper(shopId);
        List<TruckDTO> dtos = shop.getAssignedTrucks().stream().map(TruckDTO::new).toList();
        return ResponseEntity.ok(dtos);
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
        List<TruckDTO> dtos = truckService.findAllByAssignedShopIsNull().stream().map(TruckDTO::new).toList();
        return ResponseEntity.ok(dtos);
    }
}
