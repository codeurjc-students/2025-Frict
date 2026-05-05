package com.tfg.backend.controller;

import com.tfg.backend.dto.PageResponse;
import com.tfg.backend.dto.ProductDTO;
import com.tfg.backend.dto.ShopStockDTO;
import com.tfg.backend.model.Product;
import com.tfg.backend.model.ProductImageInfo;
import com.tfg.backend.service.ProductService;
import com.tfg.backend.utils.PageFormatter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Product Management", description = "Product data management")
@RequiredArgsConstructor
public class ProductRestController {

    private final ProductService productService;

    @Operation(summary = "(Admin) Get all products (paged)")
    @GetMapping("/")
    public ResponseEntity<PageResponse<ProductDTO>> getAllProducts(Pageable pageable) {
        Page<Product> products = productService.getAllProducts(pageable);
        return ResponseEntity.ok(PageFormatter.toPageResponse(products, ProductDTO::new));
    }

    @Operation(summary = "(All) Get products with applied filters (paged)")
    @GetMapping("/filter")
    public ResponseEntity<PageResponse<ProductDTO>> getFilteredProducts(
            Pageable pageable,
            @RequestParam(value = "query", required = false) String searchTerm,
            @RequestParam(value = "categoryId", required = false) List<Long> categoryIds) {
        Page<Product> products = productService.getFilteredProducts(searchTerm, categoryIds, pageable);
        return ResponseEntity.ok(PageFormatter.toPageResponse(products, ProductDTO::new));
    }

    @Operation(summary = "(User) Get logged user favourite products (paged)")
    @GetMapping("/favourites")
    public ResponseEntity<PageResponse<ProductDTO>> getUserFavouriteProducts(Pageable pageable) {
        Page<Product> favouriteProducts = productService.getUserFavouriteProducts(pageable);
        return ResponseEntity.ok(PageFormatter.toPageResponse(favouriteProducts, ProductDTO::new));
    }

    @Operation(summary = "(User) Check if a product is in logged user favourites")
    @GetMapping("/favourites/{id}")
    public ResponseEntity<Boolean> checkProductInFavourites(@PathVariable Long id) {
        boolean inFavourites = productService.checkProductInFavourites(id);
        return ResponseEntity.ok(inFavourites);
    }

    @Operation(summary = "(All) Get product by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        return ResponseEntity.ok(new ProductDTO(product));
    }

    @Operation(summary = "(All) Get product stock by ID")
    @GetMapping("/stock/{id}")
    public ResponseEntity<List<ShopStockDTO>> getProductStock(@PathVariable Long id) {
        Product product = productService.findProductHelper(id);
        List<ShopStockDTO> dtos = product.getShopsStock().stream().map(ShopStockDTO::new).toList();
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "(Manager) Get eligible products by shop ID")
    @GetMapping("/available/{shopId}")
    public ResponseEntity<List<ProductDTO>> getEligibleProducts(@PathVariable Long shopId) {
        List<ProductDTO> availableProducts = productService.findProductsNotAssignedToShop(shopId)
                .stream().map(ProductDTO::new).toList();
        return ResponseEntity.ok(availableProducts);
    }

    @Operation(summary = "(User) Add product to logged user favourites")
    @PostMapping("/favourites/{id}")
    public ResponseEntity<ProductDTO> addProductToFavourites(@PathVariable Long id) {
        Product product = productService.addProductToFavourites(id);
        return ResponseEntity.ok(new ProductDTO(product));
    }

    @Operation(summary = "(User) Delete product from logged user favourites")
    @DeleteMapping("/favourites/{id}")
    public ResponseEntity<ProductDTO> deleteProductFromFavourites(@PathVariable Long id) {
        Product product = productService.deleteProductFromFavourites(id);
        return ResponseEntity.ok(new ProductDTO(product));
    }

    @Operation(summary = "(Admin) Create product")
    @PostMapping
    public ResponseEntity<ProductDTO> createProduct(@RequestBody ProductDTO productDTO) {
        Product savedProduct = productService.createProduct(productDTO);

        // 2. Creación de URI dinámica, independiente de si la API cambia de prefijo en el futuro
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedProduct.getId())
                .toUri();

        return ResponseEntity.created(location).body(new ProductDTO(savedProduct));
    }

    @Operation(summary = "(Admin) Update product by ID")
    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> updateProduct(@PathVariable Long id, @RequestBody ProductDTO productDTO) {
        Product updatedProduct = productService.updateProduct(id, productDTO);
        return ResponseEntity.ok(new ProductDTO(updatedProduct));
    }

    @Operation(summary = "(Admin) Delete product by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<ProductDTO> deleteProduct(@PathVariable Long id) {
        Product deletedProduct = productService.deleteProduct(id);
        return ResponseEntity.ok(new ProductDTO(deletedProduct));
    }

    @Operation(summary = "(Admin) Toggle product global activation by ID")
    @PutMapping("/active/{id}")
    public ResponseEntity<ProductDTO> toggleGlobalActivation(@PathVariable Long id, @RequestParam boolean state) {
        Product updatedProduct = productService.toggleGlobalActivation(id, state);
        return ResponseEntity.ok(new ProductDTO(updatedProduct));
    }

    @Operation(summary = "(Admin) Toggle all products global activation")
    @PutMapping("/active/")
    public ResponseEntity<Boolean> toggleAllGlobalActivations(@RequestParam boolean state) {
        boolean savedState = productService.toggleAllGlobalActivations(state);
        return ResponseEntity.ok(savedState);
    }

    @Operation(summary = "(Admin) Update product images (remove unused, add new)")
    @PutMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductDTO> updateProductImages(
            @PathVariable Long id,
            @RequestPart("existingImages") List<ProductImageInfo> existingImages,
            @RequestPart(value = "newImages", required = false) List<MultipartFile> newImages
    ) {
        Product updatedProduct = productService.updateProductImages(id, existingImages, newImages);
        return ResponseEntity.ok(new ProductDTO(updatedProduct));
    }

    @Operation(summary = "(Admin) Delete remote product image by ID")
    @DeleteMapping("/{productId}/images/{imageId}")
    public ResponseEntity<ProductDTO> deleteImage(@PathVariable Long productId, @PathVariable Long imageId) {
        Product updatedProduct = productService.deleteImage(productId, imageId);
        return ResponseEntity.ok(new ProductDTO(updatedProduct));
    }
}