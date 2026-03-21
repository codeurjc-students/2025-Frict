package com.tfg.backend.service;

import com.tfg.backend.model.Order;
import com.tfg.backend.model.OrderStatus;
import com.tfg.backend.model.User;
import com.tfg.backend.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    public Optional<Order> findById(Long id){ return this.orderRepository.findById(id); }

    public Page<Order> findAll(Pageable pageable) { return orderRepository.findAll(pageable); }

    public List<Order> findAll() { return orderRepository.findAll(); }

    public Page<Order> findOrdersByManagerId(Long managerId, Pageable pageable) {
        return orderRepository.findByAssignedShop_AssignedManager_Id(managerId, pageable);
    }

    public Page<Order> findOrdersByDriverId(Long driverId, Pageable pageable) {
        return orderRepository.findByAssignedTruck_AssignedDriver_Id(driverId, pageable);
    }

    public Page<Order> findAllByUser(User u, Pageable pageInfo){
        return orderRepository.findAllByUser(u, pageInfo);
    }

    public Order save(Order o) {
        return orderRepository.save(o);
    }

    public void saveAll(Set<Order> o) {
        orderRepository.saveAll(o);
    }

    public void delete(Order o) {
        orderRepository.delete(o);
    }

    public boolean existsByIdAndUser(Long orderId, User user) {
        return this.orderRepository.existsByIdAndUser(orderId, user);
    }

    public Order findOrderHelper(Long id) {
        return this.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order with ID " + id + " does not exist."));
    }

    //Metrics
    // ADMIN, MANAGER
    public long getDashboardActiveOrders(User currentUser) {
        List<OrderStatus> statuses = List.of(OrderStatus.ORDER_MADE, OrderStatus.SENT, OrderStatus.ON_DELIVERY);

        if (currentUser.hasRole("ADMIN")) {
            return orderRepository.countOrdersByStatusIn(statuses);
        }
        return orderRepository.countOrdersByManagerIdAndStatusIn(currentUser.getId(), statuses);
    }

    // DRIVER
    public long getDriverTotalOrders(User currentUser) {
        return orderRepository.countTotalOrdersByDriverId(currentUser.getId());
    }

    public long getDriverCompletedOrders(User currentUser) {
        List<OrderStatus> completedStatuses = List.of(OrderStatus.COMPLETED);
        return orderRepository.countOrdersByDriverIdAndStatusIn(currentUser.getId(), completedStatuses);
    }

    public long getDriverPendingOrders(User currentUser) {
        List<OrderStatus> pendingStatuses = List.of(OrderStatus.ORDER_MADE, OrderStatus.SENT, OrderStatus.ON_DELIVERY);
        return orderRepository.countOrdersByDriverIdAndStatusIn(currentUser.getId(), pendingStatuses);
    }
}
