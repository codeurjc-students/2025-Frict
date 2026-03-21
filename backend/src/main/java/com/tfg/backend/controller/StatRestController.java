package com.tfg.backend.controller;

import com.tfg.backend.model.User;
import com.tfg.backend.service.OrderService;
import com.tfg.backend.service.ShopService;
import com.tfg.backend.service.TruckService;
import com.tfg.backend.service.UserService;
import com.tfg.backend.utils.StatDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stats")
@Tag(name = "Metrics Management", description = "Entity metrics management")
public class StatRestController {

    @Autowired
    private ShopService shopService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private TruckService truckService;

    @Autowired
    private UserService userService;


    @Operation(summary = "(Admin, Manager, Driver) Get order statistics by role")
    @GetMapping("/orders")
    public ResponseEntity<List<StatDTO>> getOrdersStatsByRole() {
        User loggedUser = userService.findLoggedUserHelper();
        List<StatDTO> metrics = orderService.getOrdersStatistics(loggedUser);
        return ResponseEntity.ok(metrics);
    }


    @Operation(summary = "(Admin, Manager) Get shop statistics by role")
    @GetMapping("/shops")
    public ResponseEntity<List<StatDTO>> getShopsStatsByRole() {
        User loggedUser = userService.findLoggedUserHelper();
        List<StatDTO> shopMetrics = shopService.getShopsStatistics(loggedUser);
        return ResponseEntity.ok(shopMetrics);
    }
}
