package com.tfg.backend.service;

import com.tfg.backend.model.Product;
import com.tfg.backend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepository repository;

    public List<Product> findAll() { return repository.findAll(); }

    public Product save(Product p) {
        return repository.save(p);
    }
}
