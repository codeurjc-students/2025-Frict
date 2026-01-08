package com.tfg.backend.unit;

import com.tfg.backend.model.Product;
import com.tfg.backend.repository.ProductRepository;
import com.tfg.backend.service.ProductService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

//SERVER SIDE UNIT TESTS
public class ProductServiceUTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testProduct = new Product("Cámara reflex", "Fotografías profesionales al instante", 799.99);
    }

    @Test
    public void createProductTest(){
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        when(productRepository.existsByReferenceCode(testProduct.getReferenceCode())).thenReturn(false);

        Product savedProduct = productService.save(testProduct);

        assertNotNull(savedProduct);
        assertEquals(testProduct.getReferenceCode(), savedProduct.getReferenceCode());
        assertEquals(testProduct.getName(), savedProduct.getName());
        assertEquals(testProduct.getDescription(), savedProduct.getDescription());
        assertEquals(testProduct.getCurrentPrice(), savedProduct.getCurrentPrice());

        verify(productRepository, times(1)).save(testProduct);
    }

    @Test
    public void createProduct_InvalidFieldTest(){

        Product invalidRefCode = new Product("Portátil Gamer", "Potente equipo para gaming y trabajo", 1499.99);
        Product emptyName = new Product("", "Sonido envolvente y cancelación de ruido", 199.99);
        Product nullName = new Product(null, "Monitoriza tu salud y notificaciones al instante", 249.99);
        Product negativePrice = new Product("Tablet 10\"", "Perfecta para leer, ver series y tomar notas", -329.99);

        when(productRepository.existsByReferenceCode(invalidRefCode.getReferenceCode())).thenReturn(true);

        IllegalArgumentException exInvalidRefCode = assertThrows(IllegalArgumentException.class, () -> {productService.save(invalidRefCode);});
        IllegalArgumentException exEmptyName = assertThrows(IllegalArgumentException.class, () -> {productService.save(emptyName);});
        IllegalArgumentException exNullName = assertThrows(IllegalArgumentException.class, () -> {productService.save(nullName);});
        IllegalArgumentException exNegativePrice = assertThrows(IllegalArgumentException.class, () -> {productService.save(negativePrice);});

        assertEquals("The reference code is already taken", exInvalidRefCode.getMessage());
        assertEquals("The title is null or empty", exEmptyName.getMessage());
        assertEquals("The title is null or empty", exNullName.getMessage());
        assertEquals("The price should be positive or 0", exNegativePrice.getMessage());

        verify(productRepository, times(0)).save(invalidRefCode);
        verify(productRepository, times(0)).save(emptyName);
        verify(productRepository, times(0)).save(nullName);
        verify(productRepository, times(0)).save(negativePrice);
    }

    @Test
    public void updateExistentProductTest(){
        testProduct.setId(5L);
        Optional<Product> productOpt = Optional.of(testProduct);
        Product updatedProduct = new Product("Ratón inalámbrico", "Sin necesidad de cable", 89.95);
        updatedProduct.setId(testProduct.getId());

        when(productRepository.existsById(updatedProduct.getId())).thenReturn(true);
        when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);

        Product savedProduct = productService.update(updatedProduct);

        assertEquals(5L, savedProduct.getId());
        assertEquals("Ratón inalámbrico", savedProduct.getName());
        assertEquals("Sin necesidad de cable", savedProduct.getDescription());
        assertEquals(89.95, savedProduct.getCurrentPrice());

        verify(productRepository, times(1)).save(updatedProduct);
    }

    @Test
    public void updateInexistentProductTest(){
        testProduct.setId(5L);
        when(productRepository.findById(testProduct.getId())).thenReturn(Optional.empty());
        EntityNotFoundException exInvalidId = assertThrows(EntityNotFoundException.class, () -> {productService.update(testProduct);});

        assertEquals("The product that is being updated does not exist", exInvalidId.getMessage());
        verify(productRepository, times(0)).save(testProduct);
    }

    @Test
    public void deleteExistentProductTest(){
        testProduct.setId(5L);
        when(productRepository.findById(testProduct.getId())).thenReturn(Optional.of(testProduct));

        productService.deleteById(testProduct.getId());

        verify(productRepository, times(1)).deleteById(testProduct.getId());
    }

    @Test
    public void deleteInexistentProductTest(){
        testProduct.setId(5L);
        when(productRepository.findById(testProduct.getId())).thenReturn(Optional.empty());

        EntityNotFoundException exInvalidId = assertThrows(EntityNotFoundException.class, () -> {productService.deleteById(testProduct.getId());});

        assertEquals("The product that is being deleted does not exist", exInvalidId.getMessage());
        verify(productRepository, times(0)).deleteById(testProduct.getId());
    }

}