package com.tfg.backend.integration;

import com.tfg.backend.model.Product;
import com.tfg.backend.repository.ProductRepository;
import com.tfg.backend.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

//SERVER SIDE INTEGRATION TESTS
@SpringBootTest
@ActiveProfiles("test") //Database is empty, as DatabaseInitializer class does not run with this profile
public class ProductServiceIntegrationTest {

    @Autowired
    private ProductService productService;

    Product testProduct;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp(){
        testProduct = new Product("4A5", "Portátil Gamer", null, "Potente equipo para gaming y trabajo", 1499.99);
    }

    @Test
    @Transactional
    void createProductTest() {
        Product savedProduct = productService.save(testProduct);

        assertNotNull(savedProduct);
        assertProductEquality(testProduct, savedProduct);

        Product productInDb = productService.findById(savedProduct.getId()).orElseThrow();
        assertProductEquality(testProduct, productInDb);
    }

    @Test
    @Transactional
    void updateProductTest() {
        Product savedProduct = productService.save(testProduct);
        Product modifiedProduct = new Product(testProduct.getReferenceCode(), "Patinete eléctrico", null, "Desplazamientos rápidos por ciudad", testProduct.getPrice());
        modifiedProduct.setId(savedProduct.getId());

        Product updatedProduct = productService.update(modifiedProduct);
        assertProductEquality(modifiedProduct, updatedProduct);

        Product productInDb = productService.findById(savedProduct.getId()).orElseThrow();
        assertProductEquality(productInDb, updatedProduct);
    }

    @Test
    @Transactional
    void deleteProductTest() {
        Product savedProduct = productService.save(testProduct);
        Long productId = savedProduct.getId();

        productService.deleteById(productId);

        assertAll(
                () -> assertFalse(productRepository.existsById(productId), "The product has to be deleted fron the database"),
                () -> assertTrue(productRepository.findById(productId).isEmpty(), "The product has already been found in the database")
        );
    }

    private void assertProductEquality(Product p1, Product p2) {
        assertAll(
                () -> assertEquals(p1.getReferenceCode(), p2.getReferenceCode()),
                () -> assertEquals(p1.getName(), p2.getName()),
                () -> assertEquals(p1.getDescription(), p2.getDescription()),
                () -> assertEquals(p1.getPrice(), p2.getPrice())
        );
    }











}
