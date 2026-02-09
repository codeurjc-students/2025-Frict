package com.tfg.backend.service;

import com.tfg.backend.model.ShopStock;
import com.tfg.backend.repository.ShopStockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ShopStockService {

    @Autowired
    private ShopStockRepository shopStockRepository;

    public Optional<ShopStock> findById(Long id) { return this.shopStockRepository.findById(id); }

    public List<ShopStock> findAllByShopId(Long shopId) { return this.shopStockRepository.findAllByShopId(shopId); }

    public Page<ShopStock> findAllByShopId(Long shopId, Pageable pageable) { return this.shopStockRepository.findAllByShopId(shopId, pageable); }

    public ShopStock save(ShopStock s){ return this.shopStockRepository.save(s); }

    public void saveAll(List<ShopStock> l){ this.shopStockRepository.saveAll(l); }

    public void deleteById(Long id){ this.shopStockRepository.deleteById(id); }
}
