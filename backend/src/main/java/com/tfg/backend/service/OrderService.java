package com.tfg.backend.service;

import com.tfg.backend.model.Order;
import com.tfg.backend.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    @Autowired
    private OrderRepository repository;

    public Optional<Order> findById(Long id){ return this.repository.findById(id); }

    public List<Order> findAll() { return repository.findAll(); }

    public Order save(Order o) {
        return repository.save(o);
    }
}
