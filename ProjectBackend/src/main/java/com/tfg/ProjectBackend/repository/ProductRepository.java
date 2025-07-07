package com.tfg.ProjectBackend.repository;

import com.tfg.ProjectBackend.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
