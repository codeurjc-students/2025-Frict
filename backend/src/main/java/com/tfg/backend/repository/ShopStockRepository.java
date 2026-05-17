package com.tfg.backend.repository;

import com.tfg.backend.model.ShopStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShopStockRepository extends JpaRepository<ShopStock, Long> {

    @Query("SELECT s.units FROM ShopStock s WHERE s.product.id = :productId AND s.shop.id = :shopId AND s.active = true")
    Optional<Integer> findUnitsByProductIdAndShopId(
            @Param("productId") Long productId,
            @Param("shopId") Long shopId
    );

    @Query("SELECT s FROM ShopStock s WHERE s.shop.id = :shopId AND s.product.id IN :productIds AND s.active = true")
    List<ShopStock> findStockForProductsInShop(
            @Param("shopId") Long shopId,
            @Param("productIds") List<Long> productIds
    );

    List<ShopStock> findAllByShopId(Long id);
    Page<ShopStock> findAllByShopId(Long id, Pageable p);

    Optional<ShopStock> findByShop_IdAndProduct_ReferenceCode(Long shopId, String productReferenceCode);
}
