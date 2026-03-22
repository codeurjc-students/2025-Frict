package com.tfg.backend.repository;

import com.tfg.backend.model.Order;
import com.tfg.backend.model.OrderStatus;
import com.tfg.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    boolean existsByIdAndUser(Long orderId, User user);

    Page<Order> findAllByUser(User user, Pageable pageable);

    //Orders that are assigned to the shop which is controlled by the manager
    Page<Order> findByAssignedShop_AssignedManager_Id(Long managerId, Pageable pageable);

    //Orders that are assigned to the truck which is controlled by the driver
    Page<Order> findByAssignedTruck_AssignedDriver_Id(Long driverId, Pageable pageable);

    // ==========================================
    // ADMIN METRICS
    // ==========================================

    // Count all orders that have this statuses
    @Query("""
        SELECT COUNT(o) FROM Order o 
        JOIN o.history h 
        WHERE h.id = (SELECT MAX(h2.id) FROM o.history h2) 
        AND h.status IN :statuses
    """)
    long countOrdersByStatusIn(@Param("statuses") List<OrderStatus> statuses);


    // ==========================================
    // MANAGER METRICS
    // ==========================================

    // Count all orders from the shop assigned to the manager that have this statuses
    @Query("""
        SELECT COUNT(o) FROM Order o 
        JOIN o.history h 
        WHERE o.assignedShop.assignedManager.id = :managerId 
        AND h.id = (SELECT MAX(h2.id) FROM o.history h2) 
        AND h.status IN :statuses
    """)
    long countOrdersByManagerIdAndStatusIn(@Param("managerId") Long managerId, @Param("statuses") List<OrderStatus> statuses);


    // ==========================================
    // DRIVER METRICS
    // ==========================================

    // Count all orders from the truck assigned to the driver
    @Query("SELECT COUNT(o) FROM Order o WHERE o.assignedTruck.assignedDriver.id = :driverId")
    long countTotalOrdersByDriverId(@Param("driverId") Long driverId);

    // Count all orders from the truck assigned to the driver that have this statuses
    @Query("""
        SELECT COUNT(o) FROM Order o 
        JOIN o.history h 
        WHERE o.assignedTruck.assignedDriver.id = :driverId 
        AND h.id = (SELECT MAX(h2.id) FROM o.history h2) 
        AND h.status IN :statuses
    """)
    long countOrdersByDriverIdAndStatusIn(@Param("driverId") Long driverId, @Param("statuses") List<OrderStatus> statuses);
}