package com.tfg.backend.service;
import com.tfg.backend.dto.TruckDTO;
import com.tfg.backend.model.Shop;
import com.tfg.backend.model.Truck;
import com.tfg.backend.model.User;
import com.tfg.backend.dto.EventAction;
import com.tfg.backend.event.ShopEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

//Business logic Facade design pattern. Resolves circular dependency between ShopService and TruckService classes
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ShopTruckOrchestrator {

    private final ShopService shopService;
    private final TruckService truckService;
    private final ApplicationEventPublisher eventPublisher;

    public Shop getShopByAssignedTruckId(Long truckId) {
        Truck truck = truckService.findTruckHelper(truckId);
        Shop assignedShop = truck.getAssignedShop();
        if (assignedShop == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This truck is not assigned to any shop.");
        }
        return assignedShop;
    }

    public List<Truck> getAllShopTrucks(Long shopId) {
        Shop shop = shopService.findShopHelper(shopId);
        return shop.getAssignedTrucks();
    }

    //Generates shop-type notifications
    @Transactional
    public Truck setAssignedTruck(Long shopId, Long truckId, boolean state){
        Shop shop = shopService.findShopHelper(shopId);
        Truck truck = truckService.findTruckHelper(truckId);

        if (state) {
            truck.setAssignedShop(shop);
        } else {
            truck.setAssignedShop(null);
        }

        String managerUsername = Optional.ofNullable(shop.getAssignedManager()).map(User::getUsername).orElse(null);
        List<String> driverUsername = Optional.ofNullable(truck.getAssignedDriver()).map(User::getUsername).stream().toList();
        ShopEvent shopEvent = new ShopEvent(EventAction.ASSIGNED, String.valueOf(shop.getId()), false, managerUsername, driverUsername);
        eventPublisher.publishEvent(shopEvent);

        return truck;
    }

    @Transactional
    public Truck createTruck(TruckDTO truckDTO) {
        Shop assignedShop = null;
        if (truckDTO.getShopId() != null) {
            // El orquestador obtiene la tienda legalmente a través de su servicio
            assignedShop = shopService.findShopHelper(truckDTO.getShopId());
        }
        // Le pasamos la entidad ya cargada al servicio de camiones
        return truckService.createTruck(truckDTO, assignedShop);
    }

    @Transactional
    public Truck updateTruck(Long id, TruckDTO truckDTO) {
        Shop assignedShop = null;
        if (truckDTO.getShopId() != null) {
            assignedShop = shopService.findShopHelper(truckDTO.getShopId());
        }
        return truckService.updateTruck(id, truckDTO, assignedShop);
    }
}
