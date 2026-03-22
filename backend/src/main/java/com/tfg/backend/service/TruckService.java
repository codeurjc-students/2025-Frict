package com.tfg.backend.service;

import com.tfg.backend.model.Truck;
import com.tfg.backend.model.TruckStatus;
import com.tfg.backend.model.User;
import com.tfg.backend.repository.TruckRepository;
import com.tfg.backend.utils.StatDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class TruckService {

    @Autowired
    private TruckRepository truckRepository;

    public Page<Truck> findAll(Pageable pageable) { return truckRepository.findAll(pageable); }

    public List<Truck> findAll() { return truckRepository.findAll(); }

    public List<Truck> findAllByAssignedShopIsNull() { return truckRepository.findByAssignedShopIsNull(); }

    public Optional<Truck> findById (Long id) { return truckRepository.findById(id); }

    public Page<Truck> findAllByAssignedShopId(Long shopId, Pageable pageable) { return this.truckRepository.findAllByAssignedShopId(shopId, pageable); };

    public Truck save(Truck t) { return truckRepository.save(t); }

    public List<Truck> saveAll(List<Truck> l) { return truckRepository.saveAll(l); }

    public void delete(Truck t) {
        truckRepository.delete(t);
    }

    public Truck findTruckHelper(Long id) {
        return this.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Truck with ID " + id + " does not exist."));
    }

    //Metrics
    public List<StatDTO> getTruckStats(User currentUser) {
        long available = 0;
        long onRoute = 0;
        long onMaintenance = 0;
        long outOfService = 0;

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
}
