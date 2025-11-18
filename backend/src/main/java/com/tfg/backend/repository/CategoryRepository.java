package com.tfg.backend.repository;

import com.tfg.backend.model.Category;
import com.tfg.backend.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}