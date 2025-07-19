package com.tfg.backend.repository;

import com.tfg.backend.model.Truck;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TruckRepository extends JpaRepository<Truck, Long> {
}
