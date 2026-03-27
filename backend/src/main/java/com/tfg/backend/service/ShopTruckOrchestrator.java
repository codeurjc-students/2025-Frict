package com.tfg.backend.service;
import com.tfg.backend.dto.TruckDTO;
import com.tfg.backend.model.Shop;
import com.tfg.backend.model.Truck;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

//Business logic Facade design pattern. Resolves circular dependency between ShopService and TruckService classes
@Service
@Transactional(readOnly = true)
public class ShopTruckOrchestrator {

    private final ShopService shopService;
    private final TruckService truckService;

    public ShopTruckOrchestrator(ShopService shopService, TruckService truckService) {
        this.shopService = shopService;
        this.truckService = truckService;
    }

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

    @Transactional
    public Truck setAssignedTruck(Long shopId, Long truckId, boolean state){
        Shop shop = shopService.findShopHelper(shopId);
        Truck truck = truckService.findTruckHelper(truckId);

        if (state) {
            truck.setAssignedShop(shop);
        } else {
            truck.setAssignedShop(null);
        }

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
