package com.tfg.backend.service;

import com.tfg.backend.model.Shop;
import com.tfg.backend.model.User;
import com.tfg.backend.notification.EventAction;
import com.tfg.backend.notification.UserEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ShopUserOrchestrator {

    private final ShopService shopService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

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

        //Send notifications
        UserEvent userEvent = new UserEvent(EventAction.ASSIGNED, loggedUser.getUsername());
        eventPublisher.publishEvent(userEvent);

        // Delegate to UserService to finish the action
        return userService.applyShopSelection(loggedUser, shop);
    }

    @Transactional
    public Shop setAssignedManager(Long shopId, Long userId, boolean state) {
        Shop shop = shopService.findShopHelper(shopId);

        User targetManager;
        boolean hasChanged = false;

        if (state) {
            targetManager = userService.findUserHelper(userId);
            shop.setAssignedManager(targetManager);
            hasChanged = true;
        } else {
            targetManager = shop.getAssignedManager();
            if (targetManager != null && targetManager.getId().equals(userId)) {
                shop.setAssignedManager(null);
                hasChanged = true;
            }
        }

        if (hasChanged && targetManager != null) {
            UserEvent userEvent = new UserEvent(EventAction.ASSIGNED, targetManager.getUsername());
            eventPublisher.publishEvent(userEvent);
        }

        return shop; // Updated automatically
    }
}