package com.tfg.backend.unit;

import com.tfg.backend.model.Category;
import com.tfg.backend.repository.CategoryRepository;
import com.tfg.backend.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) //Automatically connects JUnit and Mockito
class CategoryServiceUTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;


    // findAll() method tests
    @Test
    void findAll_ShouldReturnListOfCategories_WhenCategoriesExist() {
        Category cat1 = new Category();
        cat1.setId(1L);
        Category cat2 = new Category();
        cat2.setId(2L);
        List<Category> expectedList = Arrays.asList(cat1, cat2);

        when(categoryRepository.findRootsWithChildren()).thenReturn(expectedList);

        List<Category> result = categoryService.findAll();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expectedList, result);
        verify(categoryRepository, times(1)).findRootsWithChildren();
    }

    @Test
    void findAll_ShouldReturnEmptyList_WhenNoCategoriesExist() {
        when(categoryRepository.findRootsWithChildren()).thenReturn(Collections.emptyList());

        List<Category> result = categoryService.findAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(categoryRepository, times(1)).findRootsWithChildren();
    }


    // findById() method tests
    @Test
    void findById_ShouldReturnCategory_WhenIdExists() {
        long id = 1L;
        Category category = new Category();
        category.setId(id);

        when(categoryRepository.findByIdWithChildren(id)).thenReturn(Optional.of(category));

        Optional<Category> result = categoryService.findById(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
        verify(categoryRepository, times(1)).findByIdWithChildren(id);
    }

    @Test
    void findById_ShouldReturnEmpty_WhenIdDoesNotExist() {
        long id = 99L;
        when(categoryRepository.findByIdWithChildren(id)).thenReturn(Optional.empty());

        Optional<Category> result = categoryService.findById(id);

        assertTrue(result.isEmpty()); // or assertFalse(result.isPresent());
        verify(categoryRepository, times(1)).findByIdWithChildren(id);
    }


    // save() method tests
    @Test
    void save_ShouldReturnSavedCategory_WhenInputIsValid() {
        Category inputCategory = new Category();
        inputCategory.setName("New Category");

        Category savedCategory = new Category();
        savedCategory.setId(1L);
        savedCategory.setName("New Category");

        when(categoryRepository.save(inputCategory)).thenReturn(savedCategory);

        Category result = categoryService.save(inputCategory);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("New Category", result.getName());
        verify(categoryRepository, times(1)).save(inputCategory);
    }

    @Test
    void save_ShouldPropagateException_WhenRepositoryFails() {
        Category category = new Category();
        when(categoryRepository.save(any(Category.class))).thenThrow(new RuntimeException("DB Error"));

        assertThrows(RuntimeException.class, () -> categoryService.save(category));
        verify(categoryRepository, times(1)).save(category);
    }
}
