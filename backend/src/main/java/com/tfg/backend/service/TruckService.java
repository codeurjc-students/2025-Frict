package com.tfg.backend.service;

import com.tfg.backend.dto.AddressDTO;
import com.tfg.backend.dto.EventAction;
import com.tfg.backend.dto.StatDTO;
import com.tfg.backend.dto.TruckDTO;
import com.tfg.backend.event.TruckEvent;
import com.tfg.backend.model.*;
import com.tfg.backend.repository.DriverLocationRepository;
import com.tfg.backend.repository.TruckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final DriverLocationRepository driverLocationRepository;
    private final ApplicationEventPublisher eventPublisher;


    // --- READ-ONLY METHODS ---

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

    public TruckDTO toTruckDTO(Truck truck) {
        TruckDTO dto = new TruckDTO(truck);
        if (truck.getAssignedDriver() != null) {
            driverLocationRepository.findById(truck.getAssignedDriver().getUsername())
                    .ifPresent(dto::setDriverLocation);
        }
        return dto;
    }

    // --- WRITING METHODS (override Transactional) ---

    @Transactional
    public Truck save(Truck t) { return truckRepository.save(t); }

    @Transactional
    public List<Truck> saveAll(List<Truck> l) { return truckRepository.saveAll(l); }

    @Transactional
    public void delete(Truck t) { truckRepository.delete(t); }

    @Transactional
    public Truck setAssignedDriver(Long driverId, Long truckId, boolean state) {
        Truck truck = this.findTruckHelper(truckId);

        String driverUsername;
        if (state) {
            User user = userService.findUserHelper(driverId);
            truck.setAssignedDriver(user);
            driverUsername = Optional.ofNullable(user).map(User::getUsername).orElse(null);
        } else {
            driverUsername = Optional.ofNullable(truck.getAssignedDriver()).map(User::getUsername).orElse(null);
            truck.setAssignedDriver(null);
            // Copy the driver's last GPS position to truck.address so location is preserved after unassignment
            if (driverUsername != null) {
                driverLocationRepository.findById(driverUsername).ifPresent(dl -> {
                    AddressSnapshot snap = dl.getAddress();
                    Address updated = new Address(
                            "Última posición GPS",
                            snap.getStreet(), snap.getNumber(), "",
                            snap.getPostalCode(), snap.getCity(), snap.getCountry()
                    );
                    updated.setLatitude(snap.getLatitude());
                    updated.setLongitude(snap.getLongitude());
                    truck.setAddress(updated);
                });
            }
        }

        //Send in-app notifications
        String managerUsername = Optional.ofNullable(truck.getAssignedShop()).map(Shop::getAssignedManager).map(User::getUsername).orElse(null);
        TruckEvent truckEvent = new TruckEvent(EventAction.ASSIGNED, String.valueOf(truck.getId()), null, null, null, managerUsername, driverUsername);
        eventPublisher.publishEvent(truckEvent);

        return truck;
    }

    @Transactional
    public Truck commentAndOrUpdateTruckStatus(Long id, TruckStatus truckStatus, String comment) {
        Truck truck = this.findTruckHelper(id);

        //Get notification data
        String managerUsername = Optional.ofNullable(truck.getAssignedShop()).map(Shop::getAssignedManager).map(User::getUsername).orElse(null);
        String driverUsername = Optional.ofNullable(truck.getAssignedDriver()).map(User::getUsername).orElse(null);
        TruckEvent truckEvent;

        if (truckStatus == truck.getHistory().getLast().getStatus()) {
            truck.addStatusUpdate(comment);

            truckEvent = new TruckEvent(EventAction.NEW_COMMENT, String.valueOf(truck.getId()), null, null, null, managerUsername, driverUsername);

        } else {
            TruckStatus currentStatus = truck.getCurrentStatus();

            truck.changeTruckStatus(truckStatus, comment);

            truckEvent = new TruckEvent(EventAction.STATUS_CHANGED, String.valueOf(truck.getId()), currentStatus.getDescription(), truckStatus.getDescription(), null, managerUsername, driverUsername);
        }

        eventPublisher.publishEvent(truckEvent);

        return truck;
    }

    @Transactional
    public Truck createTruck(TruckDTO truckDTO, Shop assignedShop) {
        Address address = mapAddressFromDTO(truckDTO.getAddress());
        Truck truck = new Truck(truckDTO.getPlateNumber(), address, truckDTO.getMaxOrderCapacity());
        truck.setAssignedShop(assignedShop); //Can be null (valid)

        //Send in-app notifications
        String managerUsername = Optional.ofNullable(truck.getAssignedShop()).map(Shop::getAssignedManager).map(User::getUsername).orElse(null);
        TruckEvent truckEvent = new TruckEvent(EventAction.CREATED, String.valueOf(truck.getId()), null, null, null, managerUsername, null);
        eventPublisher.publishEvent(truckEvent);

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

        //Send in-app notifications
        String managerUsername = Optional.ofNullable(truck.getAssignedShop()).map(Shop::getAssignedManager).map(User::getUsername).orElse(null);
        String driverUsername = Optional.ofNullable(truck.getAssignedDriver()).map(User::getUsername).orElse(null);
        TruckEvent truckEvent = new TruckEvent(EventAction.UPDATED, String.valueOf(truck.getId()), null, null, null, managerUsername, driverUsername);
        eventPublisher.publishEvent(truckEvent);

        return truck;
    }

    @Transactional
    public Truck deleteTruck(Long id) {
        Truck truck = this.findTruckHelper(id);

        //Get notification data
        String managerUsername = Optional.ofNullable(truck.getAssignedShop()).map(Shop::getAssignedManager).map(User::getUsername).orElse(null);
        String driverUsername = Optional.ofNullable(truck.getAssignedDriver()).map(User::getUsername).orElse(null);

        // 1. Unlink shop
        if (truck.getAssignedShop() != null) {
            truck.getAssignedShop().getAssignedTrucks().remove(truck);
            truck.setAssignedShop(null);
        }

        // 2. Unlink driver
        if (truck.getAssignedDriver() != null) {
            truck.getAssignedDriver().setAssignedTruck(null);
            truck.setAssignedDriver(null);
        }

        // 3. Unlink orders
        Set<Order> linkedOrders = truck.getOrdersToDeliver();
        for (Order o : linkedOrders) {
            if (o.getHistory().getLast().getStatus() == OrderStatus.ON_DELIVERY) {
                o.changeOrderStatus(OrderStatus.SENT, "El camión ha sido borrado y el pedido ha vuelto al estado anterior.");
            }
            o.setAssignedTruck(null);
        }
        truck.getOrdersToDeliver().clear();

        // 4. Secure deletion
        truckRepository.delete(truck);

        //Send in-app notifications
        TruckEvent truckEvent = new TruckEvent(EventAction.DELETED, String.valueOf(truck.getId()), null, null, null, managerUsername, driverUsername);
        eventPublisher.publishEvent(truckEvent);

        return truck;
    }

    // --- METRICS METHODS ---

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

    // --- AUXILIARY METHODS ---

    private Address mapAddressFromDTO(AddressDTO dto) {
        Address address = new Address(dto.getAlias(), dto.getStreet(), dto.getNumber(), dto.getFloor(), dto.getPostalCode(), dto.getCity(), dto.getCountry());
        address.setLatitude(dto.getLatitude());
        address.setLongitude(dto.getLongitude());
        return address;
    }
}
