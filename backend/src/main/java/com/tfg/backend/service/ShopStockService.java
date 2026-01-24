package com.tfg.backend.service;

import com.tfg.backend.model.ShopStock;
import com.tfg.backend.repository.ShopStockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ShopStockService {

    @Autowired
    private ShopStockRepository shopStockRepository;

    public ShopStock save(ShopStock s){ return this.shopStockRepository.save(s); }
}
