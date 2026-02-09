package com.tfg.backend.repository;

import com.tfg.backend.model.Shop;
import com.tfg.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopRepository extends JpaRepository<Shop, Long> {

    Page<Shop> findAllByAssignedManagerId(Long id, Pageable pageable);
}
