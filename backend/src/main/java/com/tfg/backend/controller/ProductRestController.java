package com.tfg.backend.controller;

import com.tfg.backend.DTO.AllProductsDTO;
import com.tfg.backend.DTO.ProductDTO;
import com.tfg.backend.model.Product;
import com.tfg.backend.service.CategoryService;
import com.tfg.backend.service.ProductService;
import com.tfg.backend.utils.ImageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/products")
public class ProductRestController {

    @Autowired
    private ProductService productService;
    @Autowired
    private CategoryService categoryService;


    @GetMapping("/")
    public ResponseEntity<AllProductsDTO> getAllProducts() {
        List<Product> products = productService.findAll();
        List<ProductDTO> dtos = new ArrayList<>();
        for (Product p: products) {
            dtos.add(new ProductDTO(p));
        }
        return ResponseEntity.ok(new AllProductsDTO(dtos));
    }


    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProduct(@PathVariable Long id) {
        Optional<Product> product = productService.findById(id);
        if (!product.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new ProductDTO(product.get()));
    }


    @PostMapping
    public ResponseEntity<ProductDTO> createProduct(@RequestBody ProductDTO productDTO) {
        Product product = new Product(productDTO.getReferenceCode(), productDTO.getName(), null, productDTO.getDescription(), productDTO.getCurrentPrice());
        product.setCategories(new HashSet<>(categoryService.findAllById(productDTO.getCategoriesId())));
        Product savedProduct = productService.save(product);

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/products/{id}")
                .buildAndExpand(savedProduct.getId())
                .toUri();

        return ResponseEntity.created(location).body(new ProductDTO(savedProduct));
    }


    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> updateProduct(@PathVariable Long id, @RequestBody ProductDTO productDTO) {
        Optional<Product> productOptional = productService.findById(id);
        if (!productOptional.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Product product = productOptional.get();

        product.setReferenceCode(productDTO.getReferenceCode());
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setCurrentPrice(productDTO.getCurrentPrice());

        Product updatedProduct = productService.update(product);
        return ResponseEntity.accepted().body(new ProductDTO(updatedProduct));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<ProductDTO> deleteProduct(@PathVariable Long id) {
        Optional<Product> productOptional = productService.findById(id);
        if (!productOptional.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        Product product = productOptional.get();

        //For the moment, product relations with orders are not taken into account
        productService.deleteById(id);

        return ResponseEntity.status(200).body(new ProductDTO(product));
    }


    @GetMapping("/image/{id}")
    public ResponseEntity<byte[]> showProductImage(@PathVariable long id) {
        Optional<Product> productOptional = productService.findById(id);
        if (!productOptional.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        Product product = productOptional.get();
        return ImageUtils.serveImage(product.getProductImage());
    }


    @PutMapping("/image/{id}")
    public ResponseEntity<String> updateProductImage(@PathVariable Long id, @RequestPart("photo") MultipartFile image) {
        Optional<Product> productOptional = productService.findById(id);
        if (productOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Product product = productOptional.get();

        Blob productPhoto = ImageUtils.prepareImage(image);
        product.setProductImage(productPhoto);
        productService.save(product);
        return ResponseEntity.ok().build();
    }
}
