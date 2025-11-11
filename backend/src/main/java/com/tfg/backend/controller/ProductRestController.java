package com.tfg.backend.controller;

import com.tfg.backend.DTO.ProductsPageDTO;
import com.tfg.backend.DTO.ProductDTO;
import com.tfg.backend.model.Order;
import com.tfg.backend.model.Product;
import com.tfg.backend.service.CategoryService;
import com.tfg.backend.service.ProductService;
import com.tfg.backend.utils.ImageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    private final int pageSize = 8;


    @GetMapping("/")
    public ResponseEntity<ProductsPageDTO> getAllProducts(@RequestParam("page") int page) {
        Pageable pageable = PageRequest.of(page, Math.max(1, pageSize));
        Page<Product> products = productService.findAll(pageable);
        return ResponseEntity.ok(toProductsPageDTO(products));
    }

    @GetMapping("/filter")
    public ResponseEntity<ProductsPageDTO> getFilteredProducts(@RequestParam("page") int page,
                                                               @RequestParam(value = "query", required = false) String searchTerm,
                                                               @RequestParam(value = "categoryId", required = false) List<Long> categoryIds) {
        Pageable pageable = PageRequest.of(page, Math.max(1, pageSize));
        Page<Product> products = productService.findByFilters(searchTerm, categoryIds, pageable);
        return ResponseEntity.ok(toProductsPageDTO(products));
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
        product.setCategories(new HashSet<>(categoryService.findAllById(productDTO.getCategoriesId())));

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

        //Option 1 (active): delete intermediate table relations from Order entities -> Order info will not contain deleted products info
        //Option 2: Apply soft delete to products by adding a "deleted" boolean field -> No product removal, all orders will access products info, manage not retrieving deleted products info
        for (Order o : product.getOrders()) {
            o.getProducts().remove(product); // Quita el producto de la colección
        }
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
    public ResponseEntity<String> updateProductImage(@PathVariable Long id, @RequestPart("image") MultipartFile image) {
        Optional<Product> productOptional = productService.findById(id);
        if (productOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Product product = productOptional.get();

        Blob productImage = ImageUtils.prepareImage(image);
        product.setProductImage(productImage);
        productService.save(product);
        return ResponseEntity.ok().build();
    }

    //Creates Page<ProductDTO> objects with necessary fields only
    private ProductsPageDTO toProductsPageDTO(Page<Product> products){
        List<ProductDTO> dtos = new ArrayList<>();
        for (Product p : products.getContent()) {
            ProductDTO dto = new ProductDTO(p);
            dtos.add(dto);
        }
        return new ProductsPageDTO(dtos, products.getTotalElements(), products.getNumber(), products.getTotalPages()-1, products.getSize());
    }
}
