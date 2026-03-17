package com.tfg.backend.repository;

import com.tfg.backend.model.Order;
import com.tfg.backend.model.OrderStatus;
import com.tfg.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    boolean existsByIdAndUser(Long orderId, User user);

    Page<Order> findAllByUser(User user, Pageable pageable);

    Page<Order> findByAssignedShop_AssignedManager_Id(Long managerId, Pageable pageable);

    //Metrics
    // ADMIN: Total active orders
    @Query("""
        SELECT COUNT(o) FROM Order o 
        JOIN o.history h 
        WHERE h.id = (SELECT MAX(h2.id) FROM o.history h2) 
        AND h.status NOT IN :excludedStatuses
    """)
    long countActiveOrders(@Param("excludedStatuses") List<OrderStatus> excludedStatuses);

    // MANAGER: Assigned shops active orders
    @Query("""
        SELECT COUNT(o) FROM Order o 
        JOIN o.history h 
        WHERE o.assignedShop.assignedManager.id = :managerId 
        AND h.id = (SELECT MAX(h2.id) FROM o.history h2) 
        AND h.status NOT IN :excludedStatuses
    """)
    long countActiveOrdersByManagerId(@Param("managerId") Long managerId, @Param("excludedStatuses") List<OrderStatus> excludedStatuses);

    // DRIVER: Total orders assigned to their truck
    @Query("SELECT COUNT(o) FROM Order o WHERE o.assignedTruck.assignedDriver.id = :driverId")
    long countTotalOrdersByDriverId(@Param("driverId") Long driverId);

    // DRIVER: Orders assigned to their truck that have this order status
    @Query("""
        SELECT COUNT(o) FROM Order o 
        JOIN o.history h 
        WHERE o.assignedTruck.assignedDriver.id = :driverId 
        AND h.id = (SELECT MAX(h2.id) FROM o.history h2) 
        AND h.status IN :includedStatuses
    """)
    long countOrdersByDriverIdAndStatusIn(@Param("driverId") Long driverId, @Param("includedStatuses") List<OrderStatus> includedStatuses);

    // DRIVER: Orders assigned to their truck that do not have this order status
    @Query("""
        SELECT COUNT(o) FROM Order o 
        JOIN o.history h 
        WHERE o.assignedTruck.assignedDriver.id = :driverId 
        AND h.id = (SELECT MAX(h2.id) FROM o.history h2) 
        AND h.status NOT IN :excludedStatuses
    """)
    long countOrdersByDriverIdAndStatusNotIn(@Param("driverId") Long driverId, @Param("excludedStatuses") List<OrderStatus> excludedStatuses);
}
