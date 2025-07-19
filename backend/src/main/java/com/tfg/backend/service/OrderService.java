package com.tfg.backend.service;

import com.tfg.backend.model.Order;
import com.tfg.backend.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderRepository repository;

    public List<Order> findAll() { return repository.findAll(); }

    public Order save(Order o) {
        return repository.save(o);
    }
}
