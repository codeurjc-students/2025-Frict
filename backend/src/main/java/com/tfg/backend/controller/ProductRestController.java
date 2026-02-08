package com.tfg.backend.controller;

import com.tfg.backend.dto.*;
import com.tfg.backend.model.*;
import com.tfg.backend.service.*;
import com.tfg.backend.utils.GlobalDefaults;
import com.tfg.backend.utils.PageFormatter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Product Management", description = "Product data management")
public class ProductRestController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UserService userService;

    @Autowired
    private StorageService storageService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private ShopService shopService;


    @Operation(summary = "(Admin) Get all products (paged)")
    @GetMapping("/")
    public ResponseEntity<PageResponse<ProductDTO>> getAllProducts(Pageable pageable) {
        Page<Product> products = productService.findAll(pageable);
        return ResponseEntity.ok(PageFormatter.toPageResponse(products, ProductDTO::new));
    }


    @Operation(summary = "(All) Get products with applied filters (paged)")
    @GetMapping("/filter")
    public ResponseEntity<PageResponse<ProductDTO>> getFilteredProducts(Pageable pageable,
                                                                        @RequestParam(value = "query", required = false) String searchTerm,
                                                                        @RequestParam(value = "categoryId", required = false) List<Long> categoryIds) {
        Page<Product> products = productService.findByFilters(searchTerm, categoryIds, pageable);
        return ResponseEntity.ok(PageFormatter.toPageResponse(products, ProductDTO::new));
    }


    @Operation(summary = "(User) Get logged user favourite products (paged)")
    @GetMapping("/favourites")
    public ResponseEntity<PageResponse<ProductDTO>> getUserFavouriteProducts(HttpServletRequest request, Pageable pageable) {
        //Get logged user info if any (User class)
        User loggedUser = findLoggedUserHelper(request);

        Page<Product> favouriteProducts = productService.findUserFavouriteProductsPage(loggedUser.getId(), pageable);
        return ResponseEntity.ok(PageFormatter.toPageResponse(favouriteProducts, ProductDTO::new));
    }


    @Operation(summary = "(User) Check a product in logged user favourites")
    @GetMapping("/favourites/{id}")
    public ResponseEntity<ProductDTO> checkProductInFavourites(HttpServletRequest request, @PathVariable Long id) {
        //Get logged user info if any (User class)
        User loggedUser = findLoggedUserHelper(request);

        Product product = findProductHelper(id);
        boolean inFavourites = loggedUser.getFavouriteProducts().contains(product);

        if (!inFavourites){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product with ID " + id + " is not in favourites.");
        }
        return ResponseEntity.ok(new ProductDTO(product));
    }


    @Operation(summary = "(All) Get product by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        Product product = findProductHelper(id);
        return ResponseEntity.ok(new ProductDTO(product));
    }


    @Operation(summary = "(All) Get product stock by ID")
    @GetMapping("/stock/{id}")
    public ResponseEntity<ListResponse<ShopStockDTO>> getProductStock(@PathVariable Long id) {
        Product product = findProductHelper(id);

        List<ShopStockDTO> dtos = new ArrayList<>();
        for (ShopStock s : product.getShopsStock()) {
            dtos.add(new ShopStockDTO(s));
        }
        return ResponseEntity.ok(new ListResponse<>(dtos));
    }


    @Operation(summary = "(Manager) Get eligible products by shop ID")
    @GetMapping("/available/{shopId}")
    public ResponseEntity<List<ProductDTO>> getEligibleProducts(@PathVariable Long shopId) {
        List<ProductDTO> availableProducts = productService.findProductsNotAssignedToShop(shopId).stream().map(ProductDTO::new).toList();
        return ResponseEntity.ok(availableProducts);
    }


    @Operation(summary = "(User) Add product to logged user favourites")
    @PostMapping("/favourites/{id}")
    public ResponseEntity<ProductDTO> addProductToFavourites(HttpServletRequest request, @PathVariable Long id) {
        //Get logged user info if any (User class)
        User loggedUser = findLoggedUserHelper(request);

        //Find the product and, if exists, add it to user cart
        Product product = findProductHelper(id);

        loggedUser.getFavouriteProducts().add(product);
        userService.save(loggedUser);

        return ResponseEntity.ok(new ProductDTO(product)); //Returns the added product (optional)
    }


    @Operation(summary = "(User) Delete product from logged user favourites")
    @DeleteMapping("/favourites/{id}")
    public ResponseEntity<ProductDTO> deleteProductFromFavourites(HttpServletRequest request, @PathVariable Long id) {
        //Get logged user info if any (User class)
        User loggedUser = findLoggedUserHelper(request);

        Product product = findProductHelper(id);

        Set<Product> favouriteProducts = loggedUser.getFavouriteProducts();
        favouriteProducts.remove(product);
        userService.save(loggedUser);
        return ResponseEntity.ok().build();
    }


    @Operation(summary = "(Admin) Create product")
    @PostMapping
    public ResponseEntity<ProductDTO> createProduct(@RequestBody ProductDTO productDTO) {
        Product product = new Product(productDTO.getName(), productDTO.getDescription(), productDTO.getCurrentPrice());

        List<Category> categories = new ArrayList<>();
        for (CategoryDTO c : productDTO.getCategories()) {
            Optional<Category> categoryOptional = categoryService.findById(c.getId());
            if(categoryOptional.isEmpty()){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category with ID " + c.getId() + " does not exist.");
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


    @Operation(summary = "(Admin) Update product by ID")
    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> updateProduct(@PathVariable Long id, @RequestBody ProductDTO productDTO) {
        Product product = findProductHelper(id);
        
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setCurrentPrice(productDTO.getCurrentPrice());
        product.setActive(productDTO.isActive());

        List<Category> categories = new ArrayList<>();
        for (CategoryDTO c : productDTO.getCategories()) {
            Optional<Category> categoryOptional = categoryService.findById(c.getId());
            if(categoryOptional.isEmpty()){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category with ID " + c.getId() + " does not exist.");
            }
            categories.add(categoryOptional.get());
        }
        product.setCategories(categories);

        Product updatedProduct = productService.update(product);
        return ResponseEntity.accepted().body(new ProductDTO(updatedProduct));
    }


    @Operation(summary = "(Admin) Delete product by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<ProductDTO> deleteProduct(@PathVariable Long id) {
        Product product = findProductHelper(id);

        //Delete the Product entities, as OrderItem entities will have a product snapshot with all necessary information
        //Remove the relations not marked as CascadeType.ALL in Product
        product.getCategories().clear();

        List<OrderItem> items = orderItemService.findByProductIdAndOrderIsNotNull(product.getId());
        for (OrderItem item : items) {
            item.setProduct(null); //Unlink order items from the deleting product to be able to delete it. In case of cart items (null order), they will be deleted on cascade.
        }
        orderItemService.saveAll(items);

        productService.deleteById(id);
        return ResponseEntity.ok(new ProductDTO(product));
    }


    @Operation(summary = "(Admin) Toggle product global activation by ID")
    @PostMapping("/active/{id}")
    public ResponseEntity<ProductDTO> toggleGlobalActivation(@PathVariable Long id, @RequestParam boolean state) {
        Product product = findProductHelper(id);
        product.setActive(state);
        //If the global product state is false, it must not be in any user cart
        if (!state){
            product.getOrderItems().removeIf(item -> {
                if (item.getOrder() == null) {
                    orderItemService.delete(item);
                    return true;
                }
                return false;
            });
        }
        Product savedProduct = productService.update(product);
        return ResponseEntity.ok(new ProductDTO(savedProduct));
    }


    @Operation(summary = "(Admin) Toggle all products global activation")
    @PostMapping("/active/")
    public ResponseEntity<Boolean> toggleAllGlobalActivations(@RequestParam boolean state) {
        List<Product> products = productService.findAll();
        for (Product product : products) {
            product.setActive(state);
            if (!state){
                product.getOrderItems().removeIf(item -> {
                    if (item.getOrder() == null) {
                        orderItemService.delete(item);
                        return true;
                    }
                    return false;
                });
            }
        }
        productService.saveAll(products);
        return ResponseEntity.ok(state); //State all toggles should have in frontend
    }


    @Operation(summary = "(Admin) Update product images (remove unused, add new)")
    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductDTO> updateProductImages(
            @PathVariable Long id,
            @RequestPart("existingImages") List<ProductImageInfo> existingImages,
            @RequestPart(value = "newImages", required = false) List<MultipartFile> newImages
    ) throws IOException {

        Product product = findProductHelper(id);
        List<ProductImageInfo> currentImages = product.getImages();

        // Delete images not present in existingImages
        Set<Long> keepIds = existingImages.stream().map(ProductImageInfo::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        Iterator<ProductImageInfo> iterator = currentImages.iterator();
        while (iterator.hasNext()) {
            ProductImageInfo currentImg = iterator.next();
            if (!keepIds.contains(currentImg.getId())) {
                if(!currentImg.getS3Key().equals(GlobalDefaults.PRODUCT_IMAGE.getS3Key())){
                    storageService.deleteFile(currentImg.getS3Key());
                }
                iterator.remove();
            }
        }

        // Add new images
        if (newImages != null && !newImages.isEmpty()) {
            for (MultipartFile file : newImages) {
                Map<String, String> res = storageService.uploadFile(file, "products");

                ProductImageInfo newImg = new ProductImageInfo();
                newImg.setImageUrl(res.get("url"));
                newImg.setS3Key(res.get("key"));
                newImg.setFileName(file.getOriginalFilename());
                newImg.setProduct(product);

                currentImages.add(newImg);
            }
        }
        else if (currentImages.isEmpty()) { //No current images + No new images -> Set default image
            currentImages.add(new ProductImageInfo(GlobalDefaults.PRODUCT_IMAGE, product));
        }

        return ResponseEntity.ok(new ProductDTO(productService.update(product)));
    }


    @Operation(summary = "(Admin) Delete remote product image by ID")
    @DeleteMapping("/{productId}/images/{imageId}")
    public ResponseEntity<ProductDTO> deleteImage(@PathVariable Long productId, @PathVariable Long imageId) {
        Product product = findProductHelper(productId);

        ProductImageInfo imageToRemove = product.getImages().stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Image not found in this product."));

        // 3. Delete from MinIO
        if(!imageToRemove.getS3Key().equals(GlobalDefaults.PRODUCT_IMAGE.getS3Key())) {
            storageService.deleteFile(imageToRemove.getS3Key());
            product.getImages().remove(imageToRemove);
        }

        if(product.getImages().isEmpty()){
            product.getImages().add(new ProductImageInfo(GlobalDefaults.PRODUCT_IMAGE, product));
        }

        Product savedProduct = productService.save(product);
        return ResponseEntity.ok(new ProductDTO(savedProduct));
    }


    private User findLoggedUserHelper(HttpServletRequest request) {
        return this.userService.getLoggedUser(request)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You must be logged to perform this operation."));
    }

    private Shop findShopHelper(Long id) {
        return this.shopService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop with ID " + id + " does not exist."));
    }

    private Product findProductHelper(Long id) {
        return this.productService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product with ID " + id + " does not exist."));
    }

}
