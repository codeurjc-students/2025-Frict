package com.tfg.backend.repository;

import com.tfg.backend.model.ShopStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShopStockRepository extends JpaRepository<ShopStock, Long> {

    List<ShopStock> findAllByShopId(Long id);
    Page<ShopStock> findAllByShopId(Long id, Pageable p);
}
