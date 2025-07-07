package com.tfg.ProjectBackend.repository;

import com.tfg.ProjectBackend.model.Truck;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TruckRepository extends JpaRepository<Truck, Long> {
}
