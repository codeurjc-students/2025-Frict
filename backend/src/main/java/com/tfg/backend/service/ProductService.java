package com.tfg.backend.service;

import com.tfg.backend.model.Product;
import com.tfg.backend.repository.OrderRepository;
import com.tfg.backend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository repository;

    @Autowired
    private OrderRepository orderRepository;

    public List<Product> findAll() {
        return repository.findAll();
    }

    public Product save(Product p) {
        return repository.save(p);
    }

    public Optional<Product> findById(Long id) {
        return repository.findById(id);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}