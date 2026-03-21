package com.tfg.backend.service;

import com.tfg.backend.model.Shop;
import com.tfg.backend.model.User;
import com.tfg.backend.repository.ShopRepository;
import com.tfg.backend.utils.StatDTO;
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
    public List<StatDTO> getShopsStatistics(User currentUser) {
        long shopCount = 0;
        double totalBudget = 0.0;

        if (currentUser.hasRole("ADMIN")) {
            shopCount = shopRepository.count();
            totalBudget = shopRepository.sumAllAssignedBudgets();
        } else if (currentUser.hasRole("MANAGER")) {
            shopCount = shopRepository.countByAssignedManagerId(currentUser.getId());
            totalBudget = shopRepository.sumAssignedBudgetsByManagerId(currentUser.getId());
        } else {
            return List.of();
        }

        return List.of(
                new StatDTO("Tiendas", shopCount),
                new StatDTO("Presupuesto Total", totalBudget)
        );
    }
}
