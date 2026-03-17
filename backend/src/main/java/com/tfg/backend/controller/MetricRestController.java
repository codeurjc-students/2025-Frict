package com.tfg.backend.controller;

import com.tfg.backend.model.User;
import com.tfg.backend.service.OrderService;
import com.tfg.backend.service.ShopService;
import com.tfg.backend.service.TruckService;
import com.tfg.backend.service.UserService;
import com.tfg.backend.utils.MetricDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/metrics")
@Tag(name = "Metrics Management", description = "Entity metrics management")
public class MetricRestController {

    @Autowired
    private ShopService shopService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private TruckService truckService;

    @Autowired
    private UserService userService;


    @GetMapping("/stats")
    public ResponseEntity<List<MetricDTO>> getStats() {
        User loggedUser = userService.findLoggedUserHelper();

        List<MetricDTO> stats = new ArrayList<>();

        if (loggedUser.hasRole("DRIVER")) {
            stats.add(new MetricDTO("Total Asignados", orderService.getDriverTotalOrders(loggedUser)));
            stats.add(new MetricDTO("Completados", orderService.getDriverCompletedOrders(loggedUser)));
            stats.add(new MetricDTO("Pendientes", orderService.getDriverPendingOrders(loggedUser)));
        }
        else if (loggedUser.hasRole("ADMIN") || loggedUser.hasRole("MANAGER")) {
            stats.add(new MetricDTO("Presupuesto Total", shopService.getDashboardTotalBudget(loggedUser)));
            stats.add(new MetricDTO("Pedidos Activos", orderService.getDashboardActiveOrders(loggedUser)));
            stats.add(new MetricDTO("Camiones Operativos", truckService.getDashboardActiveTrucks(loggedUser)));
            stats.add(new MetricDTO("Tiendas", shopService.getDashboardShopCount(loggedUser)));
        }

        return ResponseEntity.ok(stats);
    }
}
