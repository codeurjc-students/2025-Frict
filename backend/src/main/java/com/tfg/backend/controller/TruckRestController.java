package com.tfg.backend.controller;

import com.tfg.backend.dto.PageResponse;
import com.tfg.backend.dto.ShopStockDTO;
import com.tfg.backend.dto.TruckDTO;
import com.tfg.backend.model.ShopStock;
import com.tfg.backend.model.Truck;
import com.tfg.backend.service.TruckService;
import com.tfg.backend.utils.PageFormatter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trucks")
@Tag(name = "Trucks Management", description = "System trucks data management")
public class TruckRestController {

    @Autowired
    private TruckService truckService;


    @Operation(summary = "(All) Get trucks page by shop ID")
    @GetMapping("/shop/{shopId}")
    public ResponseEntity<PageResponse<TruckDTO>> getTrucksByShopId(@PathVariable Long shopId, Pageable pageable) {
        Page<Truck> trucks = truckService.findAllByAssignedShopId(shopId, pageable);
        return ResponseEntity.ok(PageFormatter.toPageResponse(trucks, TruckDTO::new));
    }
}
