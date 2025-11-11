package com.tfg.backend.repository;

import com.tfg.backend.model.ShopStock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopStockRepository extends JpaRepository<ShopStock, Long> {

}
