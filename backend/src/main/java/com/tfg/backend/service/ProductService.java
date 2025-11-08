package com.tfg.backend.service;

import com.tfg.backend.model.Product;
import com.tfg.backend.model.ShopStock;
import com.tfg.backend.repository.OrderRepository;
import com.tfg.backend.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    public Page<Product> findAll(Pageable pageInfo) {
        return productRepository.findAll(pageInfo);
    }

    public Page<Product> findByFilters(String searchTerm, List<Long> categoryIds, Pageable pageInfo) {
        return productRepository.findByFilters(searchTerm, categoryIds, pageInfo);
    }

    public int findAvailableUnits(Long id) {
        Optional<Product> productOptional = productRepository.findById(id);
        if(!productOptional.isPresent()){
            return 0;
        }
        Set<ShopStock> units = productOptional.get().getShopsStock();
        int availableUnits = 0;
        for (ShopStock s : units) {
            availableUnits += s.getStock();
        }
        return availableUnits;
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
        this.checkProductFields(p);
        return productRepository.save(p);
    }

    public Product update(Product p) {
        if (!productRepository.existsById(p.getId())){
            throw new EntityNotFoundException("The product that is being updated does not exist");
        }
        this.checkProductFields(p);
        return productRepository.save(p);
    }

    public void deleteById(Long id) {
        Optional<Product> productOpt = productRepository.findById(id);
        if (!productOpt.isPresent()) {
            throw new EntityNotFoundException("The product that is being deleted does not exist");
        }
        productRepository.deleteById(id);
    }

    private void checkProductFields(Product p){
        if (p.getName() == null || p.getName().isEmpty()){
            throw new IllegalArgumentException("The title is null or empty");
        }
        else if (p.getCurrentPrice() < 0){
            throw new IllegalArgumentException("The price should be positive or 0");
        }
    }
}