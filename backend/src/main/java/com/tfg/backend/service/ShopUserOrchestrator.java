package com.tfg.backend.service;

import com.tfg.backend.model.Shop;
import com.tfg.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ShopUserOrchestrator {

    private final ShopService shopService;
    private final UserService userService;

    public ShopUserOrchestrator(ShopService shopService, UserService userService) {
        this.shopService = shopService;
        this.userService = userService;
    }

    public Page<Shop> getAssignedShopsPage(Pageable pageable) {
        User loggedUser = userService.findLoggedUserHelper();
        return shopService.findAllByAssignedManagerId(loggedUser.getId(), pageable);
    }

    @Transactional
    public boolean setSelectedShop(Map<String, Long> body) {
        User loggedUser = userService.findLoggedUserHelper();
        Long shopId = body.get("shopId");
        Shop shop = null;

        if (shopId != null) {
            shop = shopService.findShopHelper(shopId);
        }

        // Delegate to UserService to finish the action
        return userService.applyShopSelection(loggedUser, shop);
    }

    @Transactional
    public Shop setAssignedManager(Long shopId, Long userId, boolean state) {
        Shop shop = shopService.findShopHelper(shopId);

        if (state) {
            User newManager = userService.findUserHelper(userId);
            shop.setAssignedManager(newManager);
        } else {
            User currentManager = shop.getAssignedManager();
            if (currentManager != null && currentManager.getId().equals(userId)) {
                shop.setAssignedManager(null);
            }
        }

        return shop; // Updated automatically
    }
}