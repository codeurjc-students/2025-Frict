package com.tfg.ProjectBackend.repository;

import com.tfg.ProjectBackend.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopRepository extends JpaRepository<Shop, Long> {
}
