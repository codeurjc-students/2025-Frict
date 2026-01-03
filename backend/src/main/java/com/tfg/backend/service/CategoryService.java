package com.tfg.backend.service;

import com.tfg.backend.model.Category;
import com.tfg.backend.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return categoryRepository.findRootsWithChildren();
    }

    @Transactional(readOnly = true)
    public Optional<Category> findById(long id) {
        return categoryRepository.findByIdWithChildren(id);
    }

    public Category save(Category category) {
        return categoryRepository.save(category);
    }
}
