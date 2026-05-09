package com.tfg.backend.service;

import com.tfg.backend.dto.AddressDTO;
import com.tfg.backend.dto.EntityType;
import com.tfg.backend.dto.EventAction;
import com.tfg.backend.dto.ShopDTO;
import com.tfg.backend.event.RegistryEvent;
import com.tfg.backend.event.ShopEvent;
import com.tfg.backend.event.UserEvent;
import com.tfg.backend.model.*;
import com.tfg.backend.utils.GlobalDefaults;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ShopUserOrchestrator {

    private final ShopService shopService;
    private final UserService userService;
    private final ImageService imageService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Shop createShop(ShopDTO shopDTO){
        AddressDTO dto = shopDTO.getAddress();
        Address address = new Address(dto.getAlias(), dto.getStreet(), dto.getNumber(), dto.getFloor(), dto.getPostalCode(), dto.getCity(), dto.getCountry());
        address.setLatitude(dto.getLatitude());
        address.setLongitude(dto.getLongitude());

        Shop shop = new Shop(shopDTO.getName(), address, shopDTO.getAssignedBudget());

        //Send notifications
        ShopEvent shopEvent = new ShopEvent(EventAction.CREATED, String.valueOf(shop.getId()), false, null, null);
        eventPublisher.publishEvent(shopEvent);

        User loggedUser = userService.findLoggedUserHelper();
        Registry budgetRegistry = new Registry(EntityType.SHOP, RegistryType.SHOP_BUDGET, shopDTO.getAssignedBudget(), shop.getReferenceCode(), shop.getName(), loggedUser.getUsername(), loggedUser.getName(), null, null, null, null);
        eventPublisher.publishEvent(new RegistryEvent(budgetRegistry));

        return shopService.save(shop);
    }

    @Transactional
    public Shop updateShop(Long id, ShopDTO shopDTO){
        Shop shop = shopService.findShopHelper(id);
        double oldBudget = shop.getAssignedBudget();

        shop.setName(shopDTO.getName());
        AddressDTO dto = shopDTO.getAddress();
        Address address = new Address(dto.getAlias(), dto.getStreet(), dto.getNumber(), dto.getFloor(), dto.getPostalCode(), dto.getCity(), dto.getCountry());
        address.setLatitude(dto.getLatitude());
        address.setLongitude(dto.getLongitude());

        shop.setAddress(address);
        shop.setAssignedBudget(shopDTO.getAssignedBudget());

        //Send notifications
        String managerUsername = Optional.ofNullable(shop.getAssignedManager()).map(User::getUsername).orElse(null);
        List<String> driverUsernames = Optional.ofNullable(shop.getAssignedTrucks()).orElse(Collections.emptyList()).stream().map(Truck::getAssignedDriver).filter(Objects::nonNull).map(User::getUsername).toList();
        ShopEvent shopEvent = new ShopEvent(EventAction.UPDATED, String.valueOf(shop.getId()), true, managerUsername, driverUsernames);
        eventPublisher.publishEvent(shopEvent);

        User loggedUser = userService.findLoggedUserHelper();
        Registry budgetRegistry = new Registry(EntityType.SHOP, RegistryType.SHOP_BUDGET, shopDTO.getAssignedBudget() - oldBudget, shop.getReferenceCode(), shop.getName(), loggedUser.getUsername(), loggedUser.getName(), null, null, null, null);
        eventPublisher.publishEvent(new RegistryEvent(budgetRegistry));

        return shop;
    }

    @Transactional
    public Shop deleteShop(Long id){
        Shop shop = shopService.findShopHelper(id);

        //Get notification data
        String managerUsername = Optional.ofNullable(shop.getAssignedManager()).map(User::getUsername).orElse(null);
        List<String> driverUsernames = Optional.ofNullable(shop.getAssignedTrucks()).orElse(Collections.emptyList()).stream().map(Truck::getAssignedDriver).filter(Objects::nonNull).map(User::getUsername).toList();

        new ArrayList<>(shop.getAssignedTrucks()).forEach(truck -> truck.setAssignedShop(null));
        shop.getAssignedTrucks().clear();

        new ArrayList<>(shop.getAssignedOrders()).forEach(order -> {
            order.setAssignedShop(null);
            if(order.getCurrentStatus() == OrderStatus.ORDER_MADE || order.getCurrentStatus() == OrderStatus.SENT){
                order.changeOrderStatus(OrderStatus.CANCELLED, "La tienda a la que estaba asignado el pedido ha sido eliminada.");
            }
        });
        shop.getAssignedOrders().clear();

        new ArrayList<>(shop.getCustomers()).forEach(customer -> customer.setSelectedShop(null));
        shop.getCustomers().clear();

        double oldBudget = shop.getAssignedBudget();
        shopService.delete(shop);

        if (shop.getImage() != null && shop.getImage().getS3Key() != null && !GlobalDefaults.isDefaultShopImage(shop.getImage())) {
            imageService.deleteFile(shop.getImage().getS3Key());
        }

        //Send notifications
        ShopEvent shopEvent = new ShopEvent(EventAction.DELETED, String.valueOf(shop.getId()), true, managerUsername, driverUsernames);
        eventPublisher.publishEvent(shopEvent);

        User loggedUser = userService.findLoggedUserHelper();
        Registry budgetRegistry = new Registry(EntityType.SHOP, RegistryType.SHOP_BUDGET, -oldBudget, shop.getReferenceCode(), shop.getName() + " (eliminada)", loggedUser.getUsername(), loggedUser.getName(), null, null, null, null);
        eventPublisher.publishEvent(new RegistryEvent(budgetRegistry));

        return shop;
    }

    public Page<Shop> getAssignedShopsPage(Pageable pageable) {
        User loggedUser = userService.findLoggedUserHelper();
        return shopService.findAllByAssignedManagerId(loggedUser.getId(), pageable);
    }

    public List<Map<String, Object>> getManagedShopReferences() {
        User loggedUser = userService.findLoggedUserHelper();
        return shopService.findAllShopReferencesByManager(loggedUser);
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