package com.tfg.backend.service;

import com.tfg.backend.model.Shop;
import com.tfg.backend.repository.ShopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShopService {

    @Autowired
    private ShopRepository repository;

    public List<Shop> findAll() { return repository.findAll(); }

    public Shop save(Shop s) {
        return repository.save(s);
    }
}
