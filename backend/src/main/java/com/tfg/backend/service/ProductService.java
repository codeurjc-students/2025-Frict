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
    private ShopStockRepository shopStockRepository;


    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Page<Product> findAll(Pageable pageInfo) {
        return productRepository.findAll(pageInfo);
    }

    public Page<Product> findByFilters(String searchTerm, List<Long> categoryIds, Pageable pageInfo) {
        return productRepository.findByFilters(searchTerm, categoryIds, pageInfo);
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
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

    public Page<Product> findUserFavouriteProductsPage(Long id, Pageable pageable) {
        return productRepository.findFavouritesByUserId(id, pageable);
    }

    public List<Product> findProductsNotAssignedToShop(Long shopId) {
        return productRepository.findProductsNotAssignedToShop(shopId);
    }


    public List<Integer> getLocalStocks(Page<Product> page, Long shopId) {
        return getLocalStocks(page.getContent(), shopId);
    }


    public List<Integer> getLocalStocks(List<Product> products, Long shopId) {
        if (shopId == null || products.isEmpty()) {
            return products.stream().map(p -> (Integer) null).toList();
        }

        List<Long> productIds = products.stream().map(Product::getId).toList();

        // Only query to DB
        List<ShopStock> stocks = shopStockRepository.findStockForProductsInShop(shopId, productIds);

        Map<Long, Integer> stockMap = stocks.stream()
                .collect(Collectors.toMap(
                        s -> s.getProduct().getId(),
                        ShopStock::getUnits
                ));

        // Same order as original list
        return products.stream()
                .map(p -> stockMap.getOrDefault(p.getId(), 0)) // 0 if no stock in that shop
                .toList();
    }


    public Integer getLocalStock(Product product, Long shopId) {
        if (shopId == null || product == null) {
            return null;
        }
        return shopStockRepository.findUnitsByProductIdAndShopId(product.getId(), shopId).orElse(0);
    }

    public Product findProductHelper(Long id) {
        return this.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product with ID " + id + " does not exist."));
    }
}