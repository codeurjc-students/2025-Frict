package com.tfg.backend.service;

import com.tfg.backend.model.Truck;
import com.tfg.backend.repository.TruckRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TruckService {

    @Autowired
    private TruckRepository repository;

    public List<Truck> findAll() { return repository.findAll(); }

    public Truck save(Truck t) { return repository.save(t); }
}
