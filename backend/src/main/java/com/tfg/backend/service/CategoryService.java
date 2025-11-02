package com.tfg.backend.service;

import com.tfg.backend.model.Category;
import com.tfg.backend.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    public List<Category> findAllById(Set<Long> ids) {
        return categoryRepository.findAllById(ids);
    }
}
