package com.tfg.backend.service;

import com.tfg.backend.model.Shop;
import com.tfg.backend.repository.ShopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ShopService {

    @Autowired
    private ShopRepository repository;

    public List<Shop> findAll() { return repository.findAll(); }

    public Page<Shop> findAll(Pageable pageInfo) { return repository.findAll(pageInfo); }

    public Optional<Shop> findById(Long id) {
        return repository.findById(id);
    }

    public Shop save(Shop s) {
        return repository.save(s);
    }

    public void delete(Shop s) {
        repository.delete(s);
    }
}
