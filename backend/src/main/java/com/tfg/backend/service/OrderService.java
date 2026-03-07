package com.tfg.backend.service;

import com.tfg.backend.model.Order;
import com.tfg.backend.model.Truck;
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

@Service
public class OrderService {

    @Autowired
    private OrderRepository repository;

    public Optional<Order> findById(Long id){ return this.repository.findById(id); }

    public Page<Order> findAll(Pageable pageable) { return repository.findAll(pageable); }

    public List<Order> findAll() { return repository.findAll(); }

    public Page<Order> findOrdersByManagerId(Long managerId, Pageable pageable) {
        return repository.findByAssignedShop_AssignedManager_Id(managerId, pageable);
    }

    public Page<Order> findAllByUser(User u, Pageable pageInfo){
        return repository.findAllByUser(u, pageInfo);
    }

    public Order save(Order o) {
        return repository.save(o);
    }

    public boolean existsByIdAndUser(Long orderId, User user) {
        return this.repository.existsByIdAndUser(orderId, user);
    }

    public Order findOrderHelper(Long id) {
        return this.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order with ID " + id + " does not exist."));
    }
}
