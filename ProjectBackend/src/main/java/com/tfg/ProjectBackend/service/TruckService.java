package com.tfg.ProjectBackend.service;

import com.tfg.ProjectBackend.model.Truck;
import com.tfg.ProjectBackend.repository.TruckRepository;
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
