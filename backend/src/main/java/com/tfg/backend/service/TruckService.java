package com.tfg.backend.service;

import com.tfg.backend.model.Truck;
import com.tfg.backend.repository.TruckRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TruckService {

    @Autowired
    private TruckRepository truckRepository;

    public List<Truck> findAll() { return truckRepository.findAll(); }

    public Page<Truck> findAllByAssignedShopId(Long shopId, Pageable pageable) { return this.truckRepository.findAllByAssignedShopId(shopId, pageable); };

    public Truck save(Truck t) { return truckRepository.save(t); }

    public List<Truck> saveAll(List<Truck> l) { return truckRepository.saveAll(l); }
}
