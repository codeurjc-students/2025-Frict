package com.tfg.backend.service;

import com.tfg.backend.model.Order;
import com.tfg.backend.model.OrderStatus;
import com.tfg.backend.model.User;
import com.tfg.backend.repository.OrderRepository;
import com.tfg.backend.utils.StatDTO;
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

    public Page<Order> findOrdersByUser(User u, Pageable pageInfo){
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
    //Counts the orders in each status for each role (admin: all, manager: assigned shops, driver: assigned truck)
    public List<StatDTO> getOrdersStatistics(User currentUser) {
        Long userId = currentUser.getId();

        long orderMade = 0;
        long sent = 0;
        long onDelivery = 0;
        long completed = 0;

        if (currentUser.hasRole("ADMIN")) {
            orderMade = orderRepository.countOrdersByStatusIn(List.of(OrderStatus.ORDER_MADE));
            sent = orderRepository.countOrdersByStatusIn(List.of(OrderStatus.SENT));
            onDelivery = orderRepository.countOrdersByStatusIn(List.of(OrderStatus.ON_DELIVERY));
            completed = orderRepository.countOrdersByStatusIn(List.of(OrderStatus.COMPLETED));

        } else if (currentUser.hasRole("MANAGER")) {
            orderMade = orderRepository.countOrdersByManagerIdAndStatusIn(userId, List.of(OrderStatus.ORDER_MADE));
            sent = orderRepository.countOrdersByManagerIdAndStatusIn(userId, List.of(OrderStatus.SENT));
            onDelivery = orderRepository.countOrdersByManagerIdAndStatusIn(userId, List.of(OrderStatus.ON_DELIVERY));
            completed = orderRepository.countOrdersByManagerIdAndStatusIn(userId, List.of(OrderStatus.COMPLETED));

        } else if (currentUser.hasRole("DRIVER")) {
            orderMade = orderRepository.countOrdersByDriverIdAndStatusIn(userId, List.of(OrderStatus.ORDER_MADE));
            sent = orderRepository.countOrdersByDriverIdAndStatusIn(userId, List.of(OrderStatus.SENT));
            onDelivery = orderRepository.countOrdersByDriverIdAndStatusIn(userId, List.of(OrderStatus.ON_DELIVERY));
            completed = orderRepository.countOrdersByDriverIdAndStatusIn(userId, List.of(OrderStatus.COMPLETED));
        }

        return List.of(
                new StatDTO("Realizados", orderMade),
                new StatDTO("Enviados", sent),
                new StatDTO("En Reparto", onDelivery),
                new StatDTO("Completados", completed)
        );
    }
}
