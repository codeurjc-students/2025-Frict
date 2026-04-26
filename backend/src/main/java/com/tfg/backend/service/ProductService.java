package com.tfg.backend.service;

import com.tfg.backend.dto.CategoryDTO;
import com.tfg.backend.dto.ProductDTO;
import com.tfg.backend.model.*;
import com.tfg.backend.notification.EventAction;
import com.tfg.backend.notification.ProductEvent;
import com.tfg.backend.notification.TruckEvent;
import com.tfg.backend.repository.ProductRepository;
import com.tfg.backend.utils.GlobalDefaults;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UserService userService;
    private final ShopStockService shopStockService;
    private final CategoryService categoryService;
    private final ImageService imageService;
    private final OrderItemService orderItemService;
    private final ApplicationEventPublisher eventPublisher;

    // --- READ-ONLY METHODS ---

    public List<Product> getAllProducts() {
        return enrichWithStock(productRepository.findAll());
    }

    public Page<Product> getAllProducts(Pageable pageInfo) {
        Page<Product> productsPage = productRepository.findAll(pageInfo);
        enrichWithStock(productsPage.getContent());
        return productsPage;
    }

    public Page<Product> getFilteredProducts(String searchTerm, List<Long> categoryIds, Pageable pageInfo) {
        Page<Product> productsPage = productRepository.findByFilters(searchTerm, categoryIds, pageInfo);
        enrichWithStock(productsPage.getContent());
        return productsPage;
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id).map(this::enrichWithStock);
    }

    public Page<Product> getUserFavouriteProducts(Pageable pageable) {
        User loggedUser = userService.findLoggedUserHelper();
        return productRepository.findFavouritesByUserId(loggedUser.getId(), pageable);
    }

    public boolean checkProductInFavourites(Long id){
        User loggedUser = userService.findLoggedUserHelper();
        Product product = this.findProductHelper(id);
        return loggedUser.getFavouriteProducts().contains(product);
    }

    public List<Product> findProductsNotAssignedToShop(Long shopId) {
        return productRepository.findProductsNotAssignedToShop(shopId);
    }

    public Product findProductHelper(Long id) {
        return this.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product with ID " + id + " does not exist."));
    }

    // --- WRITING METHODS (override Transactional) ---

    @Transactional
    public Product save(Product p) {
        if(productRepository.existsByReferenceCode(p.getReferenceCode())){
            throw new IllegalArgumentException("The reference code is already taken");
        }
        checkProductFields(p);
        return productRepository.save(p);
    }

    @Transactional
    public Product update(Product p) {
        if (!productRepository.existsById(p.getId())){
            throw new EntityNotFoundException("The product that is being updated does not exist");
        }
        checkProductFields(p);
        return productRepository.save(p);
    }

    @Transactional
    public void deleteById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("The product that is being deleted does not exist"));
        productRepository.delete(product);
    }

    @Transactional
    public Product addProductToFavourites(Long id){
        User loggedUser = userService.findLoggedUserHelper();
        Product product = this.findProductHelper(id);

        loggedUser.getFavouriteProducts().add(product);
        return product; // Saved automatically
    }

    @Transactional
    public Product deleteProductFromFavourites(Long id){
        User loggedUser = userService.findLoggedUserHelper();
        Product product = this.findProductHelper(id);

        loggedUser.getFavouriteProducts().remove(product);
        return product; // Saved automatically
    }

    @Transactional
    public Product createProduct(ProductDTO dto){
        Product product = new Product(dto.getName(), dto.getDescription(), dto.getCurrentPrice(), dto.getSupplyPrice());
        product.setCategories(processCategories(dto.getCategories()));

        //Send notifications
        List<String> managerUsernames = product.getShopsStock().stream().map(s -> s.getShop().getAssignedManager()).filter(Objects::nonNull).map(User::getUsername).toList();
        ProductEvent productEvent = new ProductEvent(EventAction.CREATED, String.valueOf(product.getId()), managerUsernames);
        eventPublisher.publishEvent(productEvent);

        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(Long id, ProductDTO dto){
        Product product = this.findProductHelper(id);

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setSupplyPrice(dto.getSupplyPrice());
        product.setCurrentPrice(dto.getCurrentPrice());
        product.setActive(dto.isActive());

        product.setCategories(processCategories(dto.getCategories()));

        //Send notifications
        List<String> managerUsernames = product.getShopsStock().stream().map(s -> s.getShop().getAssignedManager()).filter(Objects::nonNull).map(User::getUsername).toList();
        ProductEvent productEvent = new ProductEvent(EventAction.UPDATED, String.valueOf(product.getId()), managerUsernames);
        eventPublisher.publishEvent(productEvent);

        return product; // Saved automatically
    }

    @Transactional
    public Product deleteProduct(Long id){
        Product product = this.findProductHelper(id);
        product.getCategories().clear();

        List<OrderItem> itemsToKeep = new ArrayList<>();
        List<OrderItem> itemsToDelete = new ArrayList<>();
        for (OrderItem item : product.getOrderItems()) {
            if (item.getOrder() != null) {
                item.setProduct(null);
                itemsToKeep.add(item);
            } else {
                itemsToDelete.add(item);
            }
        }

        product.getOrderItems().clear();
        orderItemService.saveAll(itemsToKeep);
        for (OrderItem cartItem : itemsToDelete) {
            orderItemService.delete(cartItem);
        }

        for (ProductImageInfo i : product.getImages()) {
            if (!GlobalDefaults.isDefaultProductImage(i)) {
                imageService.deleteFile(i.getS3Key());
            }
        }

        //Send notifications
        List<String> managerUsernames = product.getShopsStock().stream().map(s -> s.getShop().getAssignedManager()).filter(Objects::nonNull).map(User::getUsername).toList();
        ProductEvent productEvent = new ProductEvent(EventAction.DELETED, String.valueOf(product.getId()), managerUsernames);
        eventPublisher.publishEvent(productEvent);

        productRepository.delete(product);
        return product;
    }

    @Transactional
    public Product toggleGlobalActivation(Long id, boolean state){
        Product product = this.findProductHelper(id);
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

        //Send notifications
        List<String> managerUsernames = product.getShopsStock().stream().map(s -> s.getShop().getAssignedManager()).filter(Objects::nonNull).map(User::getUsername).toList();
        ProductEvent productEvent = new ProductEvent(EventAction.STATUS_CHANGED, String.valueOf(product.getId()), managerUsernames);
        eventPublisher.publishEvent(productEvent);

        return product; // Saved automatically
    }

    @Transactional
    public boolean toggleAllGlobalActivations(boolean state){
        List<Product> products = this.getAllProducts();
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

        //Send notifications
        for (Product p :products) {
            List<String> usernames = p.getShopsStock().stream().map(s -> s.getShop().getAssignedManager()).filter(Objects::nonNull).map(User::getUsername).toList();
            ProductEvent productEvent = new ProductEvent(EventAction.STATUS_CHANGED, null, usernames);
            eventPublisher.publishEvent(productEvent);
        }

        return state; // Products saved automatically
    }

    @Transactional
    public Product updateProductImages(Long id, List<ProductImageInfo> existingImages, List<MultipartFile> newImages){
        Product product = this.findProductHelper(id);
        List<ProductImageInfo> currentImages = product.getImages();

        Set<String> keepS3Keys = (existingImages != null)
                ? existingImages.stream().map(ProductImageInfo::getS3Key).filter(Objects::nonNull).collect(Collectors.toSet())
                : Collections.emptySet();

        // 1. Delete discarded ones
        Iterator<ProductImageInfo> iterator = currentImages.iterator();
        while (iterator.hasNext()) {
            ProductImageInfo currentImg = iterator.next();
            if (currentImg.getS3Key() != null && !keepS3Keys.contains(currentImg.getS3Key())) {
                if (!GlobalDefaults.isDefaultProductImage(currentImg)) {
                    imageService.deleteFile(currentImg.getS3Key());
                }
                iterator.remove();
            }
        }

        // 2. Upload new ones
        if (newImages != null && !newImages.isEmpty()) {
            for (MultipartFile file : newImages) {
                ImageInfo imageInfo = imageService.uploadImageAndGetInfo(file, "products");
                currentImages.add(new ProductImageInfo(imageInfo, product));
            }
        } else if (currentImages.isEmpty()) {
            currentImages.add(new ProductImageInfo(GlobalDefaults.getDefaultProductImage(), product));
        }

        return product;
    }

    @Transactional
    public Product deleteImage(Long productId, Long imageId) {
        Product product = this.findProductHelper(productId);

        ProductImageInfo imageToRemove = product.getImages().stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Image not found in this product."));

        if(!GlobalDefaults.isDefaultProductImage(imageToRemove)) {
            imageService.deleteFile(imageToRemove.getS3Key());
            product.getImages().remove(imageToRemove);
        }

        if(product.getImages().isEmpty()){
            product.getImages().add(new ProductImageInfo(GlobalDefaults.getDefaultProductImage(), product));
        }

        return product; // Saved automatically
    }

    // --- AUXILIARY PRIVATE METHODS ---

    private List<Category> processCategories(List<CategoryDTO> dtos) {
        Category othersCategory = categoryService.findByName("Otros")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category with name \"Otros\" does not exist."));

        if (dtos == null || dtos.isEmpty()) {
            return new ArrayList<>(List.of(othersCategory));
        }

        List<Category> categories = new ArrayList<>();
        for (CategoryDTO c : dtos) {
            Category category = categoryService.findById(c.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category with ID " + c.getId() + " does not exist."));

            if (category.getChildren().isEmpty() && !Objects.equals(category.getId(), othersCategory.getId())) {
                categories.add(category);
            }
        }

        // If all categories have been filtered (e.g. only parent nodes selected), then assign "Others" category by default
        if (categories.isEmpty()) {
            categories.add(othersCategory);
        }

        return categories;
    }

    private void checkProductFields(Product p){
        if (p.getName() == null || p.getName().isEmpty()){
            throw new IllegalArgumentException("The title is null or empty");
        }
        if (p.getCurrentPrice() < 0){
            throw new IllegalArgumentException("The price should be positive or 0");
        }
    }


    public List<Product> enrichWithStock(List<Product> products) {
        if (products.isEmpty()) {
            return products;
        }

        Optional<User> loggedUserOpt = userService.getLoggedUser();

        if (loggedUserOpt.isEmpty() || !loggedUserOpt.get().getRoles().contains("USER")) {
            products.forEach(p -> p.setAvailableUnits(0));
            return products;
        }

        User loggedUser = loggedUserOpt.get();
        Long shopId = loggedUser.getSelectedShop() != null ? loggedUser.getSelectedShop().getId() : null;
        List<Integer> stocks = shopStockService.getLocalStocks(products, shopId);

        for (int i = 0; i < products.size(); i++) {
            Integer stock = stocks.get(i);
            products.get(i).setAvailableUnits(stock != null ? stock : 0);
        }

        return products;
    }

    public Product enrichWithStock(Product product) {
        Optional<User> loggedUserOpt = userService.getLoggedUser();

        if (loggedUserOpt.isEmpty() || !loggedUserOpt.get().getRoles().contains("USER")) {
            product.setAvailableUnits(0);
            return product;
        }

        User loggedUser = loggedUserOpt.get();
        Long shopId = loggedUser.getSelectedShop() != null ? loggedUser.getSelectedShop().getId() : null;
        Integer stock = shopStockService.getLocalStock(product, shopId);

        product.setAvailableUnits(stock != null ? stock : 0);
        return product;
    }
}