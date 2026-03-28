package com.tfg.backend.service;

import com.tfg.backend.dto.AddressDTO;
import com.tfg.backend.dto.TruckDTO;
import com.tfg.backend.model.*;
import com.tfg.backend.repository.TruckRepository;
import com.tfg.backend.utils.StatDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TruckService {

    private final UserService userService;
    private final TruckRepository truckRepository;


    // --- MÉTODOS DE LECTURA ---

    public Page<Truck> findAll(Pageable pageable) { return truckRepository.findAll(pageable); }
    public List<Truck> findAll() { return truckRepository.findAll(); }
    public List<Truck> findAllByAssignedShopIsNull() { return truckRepository.findByAssignedShopIsNull(); }
    public Optional<Truck> findById(Long id) { return truckRepository.findById(id); }

    public Page<Truck> findAllByAssignedShopId(Long shopId, Pageable pageable) {
        return truckRepository.findAllByAssignedShopId(shopId, pageable);
    }

    public Truck getAssignedTruckByDriverId(Long driverId) {
        User loggedUser = userService.findUserHelper(driverId);
        return loggedUser.getAssignedTruck();
    }

    public Truck findTruckHelper(Long id) {
        return this.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Truck with ID " + id + " does not exist."));
    }

    // --- MÉTODOS DE ESCRITURA ---

    @Transactional
    public Truck save(Truck t) { return truckRepository.save(t); }

    @Transactional
    public List<Truck> saveAll(List<Truck> l) { return truckRepository.saveAll(l); }

    @Transactional
    public void delete(Truck t) { truckRepository.delete(t); }

    @Transactional
    public Truck setAssignedDriver(Long driverId, Long truckId, boolean state) {
        Truck truck = this.findTruckHelper(truckId);
        if (state) {
            User user = userService.findUserHelper(driverId);
            truck.setAssignedDriver(user);
        } else {
            truck.setAssignedDriver(null);
        }
        return truck;
    }

    @Transactional
    public Truck commentAndOrUpdateTruckStatus(Long id, TruckStatus truckStatus, String comment) {
        Truck truck = this.findTruckHelper(id);
        if (truckStatus == truck.getHistory().getLast().getStatus()) {
            truck.addStatusUpdate(comment);
        } else {
            truck.changeTruckStatus(truckStatus, comment);
        }
        return truck;
    }

    @Transactional
    public Truck createTruck(TruckDTO truckDTO, Shop assignedShop) {
        Address address = mapAddressFromDTO(truckDTO.getAddress());
        Truck truck = new Truck(truckDTO.getPlateNumber(), address, truckDTO.getMaxOrderCapacity());
        truck.setAssignedShop(assignedShop); // Puede ser null, es totalmente lícito

        return truckRepository.save(truck);
    }

    @Transactional
    public Truck updateTruck(Long id, TruckDTO truckDTO, Shop assignedShop) {
        Truck truck = this.findTruckHelper(id);

        truck.setReferenceCode(truckDTO.getReferenceCode());
        truck.setPlateNumber(truckDTO.getPlateNumber());
        truck.setAddress(mapAddressFromDTO(truckDTO.getAddress()));
        truck.setMaxOrderCapacity(truckDTO.getMaxOrderCapacity());
        truck.setAssignedShop(assignedShop);

        return truck;
    }

    @Transactional
    public Truck deleteTruck(Long id) {
        Truck truck = this.findTruckHelper(id);

        Set<Order> linkedOrders = truck.getOrdersToDeliver();
        for (Order o : linkedOrders) {
            if (o.getHistory().getLast().getStatus() == OrderStatus.ON_DELIVERY) {
                o.changeOrderStatus(OrderStatus.SENT, "El camión ha sido borrado y el pedido ha vuelto al estado anterior.");
            }
            o.setAssignedTruck(null);
        }
        truck.getOrdersToDeliver().clear();

        truckRepository.delete(truck);
        return truck;
    }

    // --- MÉTODOS DE MÉTRICAS ---

    public List<StatDTO> getTruckStatistics(User currentUser) {
        long available = 0, onRoute = 0, onMaintenance = 0, outOfService = 0;

        if (currentUser.hasRole("ADMIN")) {
            available = truckRepository.countTrucksByStatus(List.of(TruckStatus.AVAILABLE));
            onRoute = truckRepository.countTrucksByStatus(List.of(TruckStatus.ON_ROUTE));
            onMaintenance = truckRepository.countTrucksByStatus(List.of(TruckStatus.MAINTENANCE));
            outOfService = truckRepository.countTrucksByStatus(List.of(TruckStatus.OUT_OF_SERVICE));
        } else if (currentUser.hasRole("MANAGER")) {
            Long managerId = currentUser.getId();
            available = truckRepository.countTrucksByManagerIdAndStatus(managerId, List.of(TruckStatus.AVAILABLE));
            onRoute = truckRepository.countTrucksByManagerIdAndStatus(managerId, List.of(TruckStatus.ON_ROUTE));
            onMaintenance = truckRepository.countTrucksByManagerIdAndStatus(managerId, List.of(TruckStatus.MAINTENANCE));
            outOfService = truckRepository.countTrucksByManagerIdAndStatus(managerId, List.of(TruckStatus.OUT_OF_SERVICE));
        } else {
            return List.of();
        }

        return List.of(
                new StatDTO("Disponibles", available),
                new StatDTO("En Ruta", onRoute),
                new StatDTO("En mantenimiento", onMaintenance),
                new StatDTO("Fuera de servicio", outOfService)
        );
    }

    // --- MÉTODOS AUXILIARES ---

    private Address mapAddressFromDTO(AddressDTO dto) {
        Address address = new Address(dto.getAlias(), dto.getStreet(), dto.getNumber(), dto.getFloor(), dto.getPostalCode(), dto.getCity(), dto.getCountry());
        address.setLatitude(dto.getLatitude());
        address.setLongitude(dto.getLongitude());
        return address;
    }
}