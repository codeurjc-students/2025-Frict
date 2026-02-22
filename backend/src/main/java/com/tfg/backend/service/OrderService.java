package com.tfg.backend.service;

import com.tfg.backend.model.Order;
import com.tfg.backend.model.User;
import com.tfg.backend.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    @Autowired
    private OrderRepository repository;

    public Optional<Order> findById(Long id){ return this.repository.findById(id); }

    public Page<Order> findAll(Pageable pageable) { return repository.findAll(pageable); }

    public List<Order> findAll() { return repository.findAll(); }

    public Page<Order> findAllByUser(User u, Pageable pageInfo){
        return repository.findAllByUser(u, pageInfo);
    }

    public Order save(Order o) {
        return repository.save(o);
    }

    public boolean existsByIdAndUser(Long orderId, User user) {
        return this.repository.existsByIdAndUser(orderId, user);
    }
}
