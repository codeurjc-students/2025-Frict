package com.tfg.backend.controller;

import com.tfg.backend.service.StatService;
import com.tfg.backend.utils.StatDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stats")
@Tag(name = "Statistics Management", description = "Entity statistics management")
@RequiredArgsConstructor
public class StatRestController {

    private final StatService statService;


    @Operation(summary = "(Admin, Manager, Driver) Get order statistics by role")
    @GetMapping("/orders")
    public ResponseEntity<List<StatDTO>> getOrdersStatsByRole() {
        List<StatDTO> ordersStatsByRole = statService.getOrdersStatsByRole();
        return ResponseEntity.ok(ordersStatsByRole);
    }


    @Operation(summary = "(Admin, Manager) Get shop statistics by role")
    @GetMapping("/shops")
    public ResponseEntity<List<StatDTO>> getShopsStatsByRole() {
        List<StatDTO> shopsStatsByRole = statService.getShopsStatsByRole();
        return ResponseEntity.ok(shopsStatsByRole);
    }

    @Operation(summary = "(Admin, Manager) Get truck statistics by role")
    @GetMapping("/trucks")
    public ResponseEntity<List<StatDTO>> getTrucksStatsByRole() {
        List<StatDTO> trucksStatsByRole = statService.getTrucksStatsByRole();
        return ResponseEntity.ok(trucksStatsByRole);
    }
}
