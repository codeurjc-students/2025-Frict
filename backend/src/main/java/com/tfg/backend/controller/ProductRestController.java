package com.tfg.backend.controller;

import com.tfg.backend.DTO.*;
import com.tfg.backend.model.*;
import com.tfg.backend.service.CategoryService;
import com.tfg.backend.service.ProductService;
import com.tfg.backend.service.StorageService;
import com.tfg.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.*;

@RestController
@RequestMapping("/api/v1/products")
public class ProductRestController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UserService userService;

    @Autowired
    private StorageService storageService;


    @GetMapping("/")
    public ResponseEntity<ProductsPageDTO> getAllProducts(Pageable pageable) {
        Page<Product> products = productService.findAll(pageable);
        return ResponseEntity.ok(toProductsPageDTO(products));
    }

    @GetMapping("/filter")
    public ResponseEntity<ProductsPageDTO> getFilteredProducts(Pageable pageable,
                                                               @RequestParam(value = "query", required = false) String searchTerm,
                                                               @RequestParam(value = "categoryId", required = false) List<Long> categoryIds) {
        Page<Product> products = productService.findByFilters(searchTerm, categoryIds, pageable);
        return ResponseEntity.ok(toProductsPageDTO(products));
    }

    @GetMapping("/favourites")
    public ResponseEntity<ProductsPageDTO> getUserFavouriteProducts(HttpServletRequest request, Pageable pageable) {
        //Get logged user info if any (User class)
        Optional<User> userOptional = userService.getLoggedUser(request);
        if(userOptional.isEmpty()){
            return ResponseEntity.status(401).build(); //Unauthorized as not logged
        }
        User loggedUser = userOptional.get();

        Page<Product> favouriteProducts = productService.findUserFavouriteProductsPage(loggedUser.getId(), pageable);
        return ResponseEntity.ok(toProductsPageDTO(favouriteProducts));
    }

    @GetMapping("/favourites/{id}")
    public ResponseEntity<ProductDTO> checkProductInFavourites(HttpServletRequest request, @PathVariable Long id) {
        //Get logged user info if any (User class)
        Optional<User> userOptional = userService.getLoggedUser(request);
        if(userOptional.isEmpty()){
            return ResponseEntity.status(401).build(); //Unauthorized as not logged
        }
        User loggedUser = userOptional.get();

        Optional<Product> productOptional = productService.findById(id);
        if(productOptional.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        Product product = productOptional.get();
        boolean inFavourites = loggedUser.getFavouriteProducts().contains(product);

        if (!inFavourites){
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(new ProductDTO(product));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        Optional<Product> product = productService.findById(id);
        if (!product.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new ProductDTO(product.get()));
    }


    @GetMapping("/stock/{id}")
    public ResponseEntity<ShopStockListDTO> getProductStock(@PathVariable Long id) {
        Optional<Product> productOptional = productService.findById(id);
        if (!productOptional.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        Product product = productOptional.get();

        List<ShopStockDTO> dtos = new ArrayList<>();
        for (ShopStock s : product.getShopsStock()) {
            dtos.add(new ShopStockDTO(s));
        }
        return ResponseEntity.ok(new ShopStockListDTO(dtos));
    }


    @PostMapping("/favourites/{id}")
    public ResponseEntity<ProductDTO> addProductToFavourites(HttpServletRequest request, @PathVariable Long id) {
        //Get logged user info if any (User class)
        Optional<User> userOptional = userService.getLoggedUser(request);
        if(userOptional.isEmpty()){
            return ResponseEntity.status(401).build(); //Unauthorized as not logged
        }
        User loggedUser = userOptional.get();

        //Find the product and, if exists, add it to user cart
        Optional<Product> productOptional = productService.findById(id);
        if(productOptional.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        Product product = productOptional.get();

        loggedUser.getFavouriteProducts().add(product);
        userService.save(loggedUser);

        return ResponseEntity.ok(new ProductDTO(product)); //Returns the added product (optional)
    }


    @DeleteMapping("/favourites/{id}")
    public ResponseEntity<ProductDTO> deleteProductFromFavourites(HttpServletRequest request, @PathVariable Long id) {
        //Get logged user info if any (User class)
        Optional<User> userOptional = userService.getLoggedUser(request);
        if(userOptional.isEmpty()){
            return ResponseEntity.status(401).build(); //Unauthorized as not logged
        }
        User loggedUser = userOptional.get();

        Optional<Product> productOptional = productService.findById(id);

        if(productOptional.isEmpty()){
            return ResponseEntity.notFound().build();
        }

        Set<Product> favouriteProducts = loggedUser.getFavouriteProducts();
        favouriteProducts.remove(productOptional.get());
        userService.save(loggedUser);
        return ResponseEntity.ok().build();
    }


    @PostMapping
    public ResponseEntity<ProductDTO> createProduct(@RequestBody ProductDTO productDTO) {
        Product product = new Product(productDTO.getName(), productDTO.getDescription(), productDTO.getCurrentPrice());

        List<Category> categories = new ArrayList<>();
        for (CategoryDTO c : productDTO.getCategories()) {
            Optional<Category> categoryOptional = categoryService.findById(c.getId());
            if(categoryOptional.isEmpty()){
                return ResponseEntity.badRequest().build();
            }
            categories.add(categoryOptional.get());
        }
        product.setCategories(categories);

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
        
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setCurrentPrice(productDTO.getCurrentPrice());

        List<Category> categories = new ArrayList<>();
        for (CategoryDTO c : productDTO.getCategories()) {
            Optional<Category> categoryOptional = categoryService.findById(c.getId());
            if(categoryOptional.isEmpty()){
                return ResponseEntity.badRequest().build();
            }
            categories.add(categoryOptional.get());
        }
        product.setCategories(categories);

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

        //Remove the relations not marked as CascadeType.ALL in Product
        product.getCategories().clear();
        product.getOrderItems().clear();

        productService.deleteById(id);
        return ResponseEntity.status(200).body(new ProductDTO(product));
    }

    @PostMapping("/{id}/images")
    public ResponseEntity<Product> uploadProductImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        Product product = productService.findById(id).orElseThrow();

        // 1. Upload
        Map<String, String> res = storageService.uploadFile(file, "products");

        // 2. Create ProductImage entity
        ProductImageInfo img = new ProductImageInfo();
        img.setImageUrl(res.get("url"));
        img.setS3Key(res.get("key"));
        img.setFileName(file.getOriginalFilename());

        // 3. Add to the list
        product.getImages().add(img);

        return ResponseEntity.ok(productService.save(product));
    }

    @DeleteMapping("/{productId}/images/{imageId}")
    public ResponseEntity<Product> deleteImage(@PathVariable Long productId, @PathVariable Long imageId) {
        // 1. Retrieve the product
        Product product = productService.findById(productId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        // 2. Find the image in product images list
        ProductImageInfo imageToRemove = product.getImages().stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Imagen no encontrada en este producto"));

        // 3. Delete from MinIO
        storageService.deleteFile(imageToRemove.getS3Key());

        // 4. Delete from image list
        product.getImages().remove(imageToRemove);

        // 5. Save product (orphanRemoval erases the image from BD)
        Product savedProduct = productService.save(product);

        return ResponseEntity.ok(savedProduct);
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
