package com.tfg.backend.service;

import com.tfg.backend.model.Product;
import com.tfg.backend.model.ShopStock;
import com.tfg.backend.repository.ShopStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShopStockService {

    private final ShopStockRepository shopStockRepository;


    public Optional<ShopStock> findById(Long id) { return this.shopStockRepository.findById(id); }

    public List<ShopStock> findAllByShopId(Long shopId) { return this.shopStockRepository.findAllByShopId(shopId); }

    public Page<ShopStock> findAllByShopId(Long shopId, Pageable pageable) { return this.shopStockRepository.findAllByShopId(shopId, pageable); }

    public ShopStock save(ShopStock s){ return this.shopStockRepository.save(s); }

    public void saveAll(List<ShopStock> l){ this.shopStockRepository.saveAll(l); }

    @Transactional
    public void deleteById(Long id){ this.shopStockRepository.deleteById(id); }

    public ShopStock findShopStockHelper(Long id) {
        return this.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock with ID " + id + " does not exist."));
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
                        s -> s.isActive() ? s.getUnits() : -1
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
        return shopStockRepository.findByProduct_IdAndShop_Id(product.getId(), shopId)
                .map(s -> s.isActive() ? s.getUnits() : -1)
                .orElse(null);
    }
}
