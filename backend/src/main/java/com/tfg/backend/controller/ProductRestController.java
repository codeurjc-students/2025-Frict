package com.tfg.backend.controller;

import com.tfg.backend.DTO.AllProductsDTO;
import com.tfg.backend.DTO.ProductDTO;
import com.tfg.backend.model.Order;
import com.tfg.backend.model.Product;
import com.tfg.backend.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/products")
public class ProductRestController {

    @Autowired
    private ProductService productService;


    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> showProduct(@PathVariable Long id) {
        Optional<Product> product = productService.findById(id);
        if (product.isPresent()) {
            return ResponseEntity.ok(new ProductDTO(product.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }


    @GetMapping("/all")
    public ResponseEntity<AllProductsDTO> showAllProducts() {
        List<Product> products = productService.findAll();
        List<ProductDTO> dtos = new ArrayList<>();
        for (Product p: products) {
            dtos.add(new ProductDTO(p));
        }
        return ResponseEntity.ok(new AllProductsDTO(dtos));
    }

    //Auxiliary method that easily allows to check a product default image
    @GetMapping("/image/{id}")
    public ResponseEntity<byte[]> showProductImage(@PathVariable long id) {
        Optional<Product> productOptional = productService.findById(id);
        if (productOptional.isPresent()) {
            Product product = productOptional.get();

            try {
                Blob photoBlob = product.getPhoto();
                byte[] photoBytes = photoBlob.getBytes(1, (int) photoBlob.length());

                return ResponseEntity
                        .ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(photoBytes);
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }


    @PostMapping
    public ResponseEntity<ProductDTO> createProduct(@RequestBody ProductDTO productDTO) {
        Product product = new Product(productDTO.getReferenceCode(), productDTO.getName(), null, productDTO.getDescription(), productDTO.getPrice());
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
        product.setPrice(productDTO.getPrice());

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
}
