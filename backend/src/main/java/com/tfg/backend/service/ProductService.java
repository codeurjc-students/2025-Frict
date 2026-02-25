package com.tfg.backend.service;

import com.tfg.backend.model.Product;
import com.tfg.backend.model.ShopStock;
import com.tfg.backend.model.User;
import com.tfg.backend.repository.OrderRepository;
import com.tfg.backend.repository.ProductRepository;
import com.tfg.backend.repository.ShopStockRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ShopStockService shopStockService;


    public List<Product> findAll() {
        List<Product> products = productRepository.findAll();
        return enrichWithStock(products);
    }

    public Page<Product> findAll(Pageable pageInfo) {
        Page<Product> productsPage = productRepository.findAll(pageInfo);
        enrichWithStock(productsPage.getContent()); // Modificamos las entidades de la página directamente
        return productsPage;
    }

    public Page<Product> findByFilters(String searchTerm, List<Long> categoryIds, Pageable pageInfo) {
        Page<Product> productsPage = productRepository.findByFilters(searchTerm, categoryIds, pageInfo);
        enrichWithStock(productsPage.getContent());
        return productsPage;
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id).map(this::enrichWithStock);
    }

    //Check that the reference code is not being used yet and all product fields are valid
    public Product save(Product p) {
        //Check the reference code is unique
        if(productRepository.existsByReferenceCode(p.getReferenceCode())){
            throw new IllegalArgumentException("The reference code is already taken");
        }
        this.checkProductFields(p);
        return productRepository.save(p);
    }

    public void saveAll(List<Product> products) {
        this.productRepository.saveAll(products);
    }

    public Product update(Product p) {
        if (!productRepository.existsById(p.getId())){
            throw new EntityNotFoundException("The product that is being updated does not exist");
        }
        this.checkProductFields(p);
        return productRepository.save(p);
    }

    public void deleteById(Long id) {
        Optional<Product> productOpt = productRepository.findById(id);
        if (!productOpt.isPresent()) {
            throw new EntityNotFoundException("The product that is being deleted does not exist");
        }
        productRepository.deleteById(id);
    }

    private void checkProductFields(Product p){
        if (p.getName() == null || p.getName().isEmpty()){
            throw new IllegalArgumentException("The title is null or empty");
        }
        else if (p.getCurrentPrice() < 0){
            throw new IllegalArgumentException("The price should be positive or 0");
        }
    }


    public List<Product> enrichWithStock(List<Product> products) {
        if (products.isEmpty()) {
            return products;
        }

        Optional<User> loggedUserOpt = userService.getLoggedUser();

        // If not logged or logged but not with the USER role, then set all available units as 0
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

        // If not logged or logged but not with the USER role, then set all available units as 0
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


    public Page<Product> findUserFavouriteProductsPage(Long id, Pageable pageable) {
        return productRepository.findFavouritesByUserId(id, pageable);
    }

    public List<Product> findProductsNotAssignedToShop(Long shopId) {
        return productRepository.findProductsNotAssignedToShop(shopId);
    }

    public Product findProductHelper(Long id) {
        return this.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product with ID " + id + " does not exist."));
    }
}