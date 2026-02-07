package com.tfg.backend.repository;

import com.tfg.backend.model.ShopStock;
import com.tfg.backend.model.Truck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TruckRepository extends JpaRepository<Truck, Long> {

    Page<Truck> findAllByAssignedShopId(Long id, Pageable p);
}
