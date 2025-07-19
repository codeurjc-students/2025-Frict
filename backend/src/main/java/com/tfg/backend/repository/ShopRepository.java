package com.tfg.backend.repository;

import com.tfg.backend.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopRepository extends JpaRepository<Shop, Long> {
}
