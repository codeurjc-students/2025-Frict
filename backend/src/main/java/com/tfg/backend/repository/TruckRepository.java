package com.tfg.backend.repository;

import com.tfg.backend.model.Truck;
import com.tfg.backend.model.TruckStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TruckRepository extends JpaRepository<Truck, Long> {

    List<Truck> findByAssignedShopIsNull();

    Page<Truck> findAllByAssignedShopId(Long id, Pageable p);

    //Metrics
    // ADMIN: Total count of trucks that do not have this statuses
    @Query("""
        SELECT COUNT(t) FROM Truck t 
        JOIN t.history h 
        WHERE h.id = (SELECT MAX(h2.id) FROM t.history h2) 
        AND h.status IN :statuses
    """)
    long countTrucksByStatus(@Param("statuses") List<TruckStatus> statuses);

    // MANAGER: Count of all trucks assigned to their shops that do not have this statuses
    @Query("""
        SELECT COUNT(t) FROM Truck t 
        JOIN t.history h 
        WHERE t.assignedShop.assignedManager.id = :managerId 
        AND h.id = (SELECT MAX(h2.id) FROM t.history h2) 
        AND h.status IN :statuses
    """)
    long countTrucksByManagerIdAndStatus(@Param("managerId") Long managerId, @Param("statuses") List<TruckStatus> statuses);
}
