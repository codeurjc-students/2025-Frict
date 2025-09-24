package com.tfg.backend.service;

import com.tfg.backend.model.Product;
import com.tfg.backend.repository.OrderRepository;
import com.tfg.backend.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    //Check that the reference code is not being used yet and all product fields are valid
    public Product save(Product p) {
        //Comprueba que el número de referencia es único
        if(productRepository.existsByReferenceCode(p.getReferenceCode())){
            throw new IllegalArgumentException("The reference code is already taken");
        }
        else if (p.getName() == null || p.getName().isEmpty()){
            throw new IllegalArgumentException("The title is null or empty");
        }
        else if (p.getPrice() < 0){
            throw new IllegalArgumentException("The price should be positive or 0");
        }
        else return productRepository.save(p);
    }

    public Product update(Product product) {
        Optional<Product> productOpt = productRepository.findById(product.getId());
        if (!productOpt.isPresent()) {
            throw new EntityNotFoundException("The product that is being updated does not exist");
        }
        return this.save(product);
    }

    public void deleteById(Long id) {
        Optional<Product> productOpt = productRepository.findById(id);
        if (!productOpt.isPresent()) {
            throw new EntityNotFoundException("The product that is being deleted does not exist");
        }
        productRepository.deleteById(id);
    }
}