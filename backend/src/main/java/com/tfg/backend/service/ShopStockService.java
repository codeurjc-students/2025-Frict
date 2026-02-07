package com.tfg.backend.service;

import com.tfg.backend.model.ShopStock;
import com.tfg.backend.repository.ShopStockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ShopStockService {

    @Autowired
    private ShopStockRepository shopStockRepository;

    public Page<ShopStock> findAllByShopId(Long shopId, Pageable pageable) { return this.shopStockRepository.findAllByShopId(shopId, pageable); };

    public ShopStock save(ShopStock s){ return this.shopStockRepository.save(s); }
}
