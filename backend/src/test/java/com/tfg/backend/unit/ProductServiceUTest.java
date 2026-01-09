package com.tfg.backend.unit;


import com.tfg.backend.model.Product;
import com.tfg.backend.repository.ProductRepository;
import com.tfg.backend.service.ProductService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

//SERVER SIDE UNIT TESTS
@ExtendWith(MockitoExtension.class)
class ProductServiceUTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Product("Cámara reflex", "Fotografías profesionales", 799.99);
        testProduct.setId(1L);
    }


    // findAll() method tests
    @Test
    void findAll_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> expectedPage = new PageImpl<>(Collections.singletonList(testProduct));
        when(productRepository.findAll(pageable)).thenReturn(expectedPage);

        Page<Product> result = productService.findAll(pageable);

        assertEquals(expectedPage, result);
        verify(productRepository).findAll(pageable);
    }


    // findById() method tests
    @Test
    void findById_ShouldReturnProduct_WhenExists() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        Optional<Product> result = productService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(testProduct, result.get());
    }


    // save() method tests
    @Test
    void save_ShouldReturnProduct_WhenValid() {
        when(productRepository.existsByReferenceCode(testProduct.getReferenceCode())).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        Product savedProduct = productService.save(testProduct);

        assertNotNull(savedProduct);
        assertEquals(testProduct.getName(), savedProduct.getName());
        verify(productRepository).existsByReferenceCode(testProduct.getReferenceCode());
        verify(productRepository).save(testProduct);
    }

    @Test
    void save_ShouldThrowException_WhenReferenceCodeExists() {
        when(productRepository.existsByReferenceCode(testProduct.getReferenceCode())).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> productService.save(testProduct));

        assertEquals("The reference code is already taken", ex.getMessage());
        verify(productRepository, never()).save(any());
    }

    @ParameterizedTest(name = "{index} => {1}")
    @MethodSource("invalidProductsProvider")
    void save_ShouldThrowException_WhenFieldsAreInvalid(Product invalidProduct, String expectedMessage) {
        lenient().when(productRepository.existsByReferenceCode(any())).thenReturn(false); // Conditional behaviour definition

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> productService.save(invalidProduct));

        assertEquals(expectedMessage, ex.getMessage());
        verify(productRepository, never()).save(any());
    }


    // update() method tests
    @Test
    void update_ShouldReturnUpdatedProduct_WhenExists() {
        Product inputProduct = new Product("Updated Name", "Desc", 100.0);
        inputProduct.setId(1L);

        when(productRepository.existsById(1L)).thenReturn(true);
        when(productRepository.save(inputProduct)).thenReturn(inputProduct);

        Product result = productService.update(inputProduct);

        assertEquals("Updated Name", result.getName());
        verify(productRepository).existsById(1L);
        verify(productRepository).save(inputProduct);
    }

    @Test
    void update_ShouldThrowException_WhenDoesNotExist() {
        Product inputProduct = new Product("Name", "Desc", 10.0);
        inputProduct.setId(99L); // Inexistent id

        when(productRepository.existsById(99L)).thenReturn(false);

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> productService.update(inputProduct));

        assertEquals("The product that is being updated does not exist", ex.getMessage());
        verify(productRepository, never()).save(any());
    }


    // deleteById() method tests
    @Test
    void deleteById_ShouldDelete_WhenExists() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        productService.deleteById(1L);

        verify(productRepository).deleteById(1L);
    }

    @Test
    void deleteById_ShouldThrowException_WhenDoesNotExist() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> productService.deleteById(1L));

        assertEquals("The product that is being deleted does not exist", ex.getMessage());
        verify(productRepository, never()).deleteById(any());
    }


    // Parametrized data provider
    static Stream<Arguments> invalidProductsProvider() {
        return Stream.of(
                Arguments.of(new Product("", "Desc", 10.0), "The title is null or empty"),
                Arguments.of(new Product(null, "Desc", 10.0), "The title is null or empty"),
                Arguments.of(new Product("Valid", "Desc", -1.0), "The price should be positive or 0")
        );
    }
}