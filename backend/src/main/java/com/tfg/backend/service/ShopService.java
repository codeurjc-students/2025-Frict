package com.tfg.backend.service;

import com.tfg.backend.model.Shop;
import com.tfg.backend.model.User;
import com.tfg.backend.repository.ShopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class ShopService {

    @Autowired
    private ShopRepository shopRepository;

    public List<Shop> findAll() { return shopRepository.findAll(); }

    public Page<Shop> findAll(Pageable pageInfo) { return shopRepository.findAll(pageInfo); }

    public Page<Shop> findAllByAssignedManagerId(Long userId, Pageable pageInfo) { return shopRepository.findAllByAssignedManagerId(userId, pageInfo); }

    public Optional<Shop> findById(Long id) {
        return shopRepository.findById(id);
    }

    public Shop save(Shop s) {
        return shopRepository.save(s);
    }

    public void delete(Shop s) {
        shopRepository.delete(s);
    }

    public Shop findShopHelper(Long id) {
        return this.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop with ID " + id + " does not exist."));
    }

    //Metrics
    public long getDashboardShopCount(User currentUser) {
        if (currentUser.hasRole("ADMIN")) {
            return shopRepository.count();
        }
        return shopRepository.countByAssignedManagerId(currentUser.getId());
    }

    public double getDashboardTotalBudget(User currentUser) {
        if (currentUser.hasRole("ADMIN")) {
            return shopRepository.sumAllAssignedBudgets();
        }
        return shopRepository.sumAssignedBudgetsByManagerId(currentUser.getId());
    }
}
