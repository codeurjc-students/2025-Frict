package com.tfg.backend.repository;

import com.tfg.backend.model.Shop;
import com.tfg.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface ShopRepository extends JpaRepository<Shop, Long> {

    Page<Shop> findAllByAssignedManagerId(Long id, Pageable pageable);

    //Metrics
    // Admin: Count all shops
    long count();

    // MANAGER: Count of assigned shops
    @Query("SELECT COUNT(s) FROM Shop s WHERE s.assignedManager.id = :managerId")
    long countByAssignedManagerId(@Param("managerId") Long managerId);

    // ADMIN: Total budget from all shops
    @Query("SELECT COALESCE(SUM(s.assignedBudget), 0) FROM Shop s")
    double sumAllAssignedBudgets();

    // MANAGER: Total budget from assigned shops
    @Query("SELECT COALESCE(SUM(s.assignedBudget), 0) FROM Shop s WHERE s.assignedManager.id = :managerId")
    double sumAssignedBudgetsByManagerId(@Param("managerId") Long managerId);
}
