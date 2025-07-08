package com.tfg.ProjectBackend.service;

import com.tfg.ProjectBackend.model.Shop;
import com.tfg.ProjectBackend.repository.ShopRepository;
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
