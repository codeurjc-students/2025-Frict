package com.tfg.backend.repository;

import com.tfg.backend.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByReferenceCode(String referenceCode);
}
