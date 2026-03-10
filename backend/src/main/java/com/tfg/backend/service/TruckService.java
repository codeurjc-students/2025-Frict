package com.tfg.backend.service;

import com.tfg.backend.model.Shop;
import com.tfg.backend.model.Truck;
import com.tfg.backend.repository.TruckRepository;
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
}
