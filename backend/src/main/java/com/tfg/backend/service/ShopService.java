package com.tfg.backend.service;

import com.tfg.backend.dto.EntityType;
import com.tfg.backend.dto.EventAction;
import com.tfg.backend.dto.StatDTO;
import com.tfg.backend.event.RegistryEvent;
import com.tfg.backend.event.ShopEvent;
import com.tfg.backend.event.ShopStockEvent;
import com.tfg.backend.model.*;
import com.tfg.backend.repository.ShopRepository;
import com.tfg.backend.utils.GlobalDefaults;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ShopService {

    private final ImageService imageService;
    private final ShopStockService shopStockService;
    private final ProductService productService;
    private final ShopRepository shopRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${notifications.stock.low-threshold:5}")
    private int lowStockThreshold;

    // --- READ-ONLY METHODS ---

    public List<Shop> findAll() { return shopRepository.findAll(); }
    public Page<Shop> findAll(Pageable pageInfo) { return shopRepository.findAll(pageInfo); }
    public Page<Shop> findAllByAssignedManagerId(Long userId, Pageable pageInfo) { return shopRepository.findAllByAssignedManagerId(userId, pageInfo); }
    public Optional<Shop> findById(Long id) { return shopRepository.findById(id); }
    public List<Map<String, Object>> findAllShopReferencesByManager(User manager){ return shopRepository.findAllShopReferencesByManager(manager); }

    public Shop findShopHelper(Long id) {
        return this.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop with ID " + id + " does not exist."));
    }

    // --- WRITING METHODS ---

    @Transactional
    public Shop save(Shop s) { return shopRepository.save(s); }

    @Transactional
    public void delete(Shop s) { shopRepository.delete(s); }

    @Transactional
    public ShopStock toggleLocalActivation(Long id, boolean state){
        ShopStock stock = shopStockService.findShopStockHelper(id);
        stock.setActive(state);

        //Send notifications
        ShopEvent shopEvent = new ShopEvent(EventAction.STATUS_CHANGED, String.valueOf(stock.getShop().getId()), true, null, null);
        eventPublisher.publishEvent(shopEvent);

        return stock;
    }

    @Transactional
    public boolean toggleAllLocalActivations(Long shopId, boolean state){
        List<ShopStock> stocks = this.shopStockService.findAllByShopId(shopId);
        stocks.forEach(s -> s.setActive(state));

        //Send notifications
        ShopEvent shopEvent = new ShopEvent(EventAction.STATUS_CHANGED, String.valueOf(shopId), true, null, null);
        eventPublisher.publishEvent(shopEvent);
        return state;
    }

    @Transactional
    public ShopStock restockProduct(Long stockId, int units){
        ShopStock targetStock = shopStockService.findShopStockHelper(stockId);

        if (units <= 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Restock units must be positive, " + units + " is not a valid number.");
        }

        Shop restockingShop = targetStock.getShop();
        double supplyCost = units * targetStock.getProduct().getSupplyPrice();
        if (supplyCost > restockingShop.getAssignedBudget()){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "There is not enough budget in this shop to complete this operation.");
        }

        double addedCapacity = targetStock.getProduct().getCapacity() * units;
        if (restockingShop.getMaxCapacity() > 0 &&
                restockingShop.getOccupiedCapacity() + addedCapacity > restockingShop.getMaxCapacity()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No hay suficiente capacidad de almacenamiento en la tienda.");
        }

        int oldUnits = targetStock.getUnits();
        int newUnits = oldUnits + units;
        targetStock.setUnits(newUnits);
        restockingShop.setAssignedBudget(restockingShop.getAssignedBudget() - supplyCost);
        restockingShop.setOccupiedCapacity(restockingShop.getOccupiedCapacity() + addedCapacity);

        //Send notifications
        ShopEvent shopEvent = new ShopEvent(EventAction.STATUS_CHANGED, String.valueOf(targetStock.getId()), true, null, null);
        eventPublisher.publishEvent(shopEvent);

        // Specific RESTOCKED event with product + shop context for tailored, role-aware notifications
        Product product = targetStock.getProduct();
        String managerUsername = restockingShop.getAssignedManager() != null ? restockingShop.getAssignedManager().getUsername() : null;
        ShopStockEvent stockEvent = new ShopStockEvent(
                ShopStockEvent.StockAction.RESTOCKED,
                restockingShop.getId(), restockingShop.getName(), restockingShop.getReferenceCode(),
                product.getId(), product.getName(), product.getReferenceCode(),
                oldUnits, newUnits, lowStockThreshold,
                managerUsername
        );
        eventPublisher.publishEvent(stockEvent);

        Registry budgetRegistry = new Registry(EntityType.SHOP, RegistryType.SHOP_BUDGET, -supplyCost, restockingShop.getReferenceCode(), restockingShop.getName(), restockingShop.getAssignedManager().getUsername(), restockingShop.getAssignedManager().getName(), product.getReferenceCode(), product.getName(), null, null);
        eventPublisher.publishEvent(new RegistryEvent(budgetRegistry));

        Registry stockRegistry = new Registry(EntityType.SHOP, RegistryType.SHOP_STOCK, (double) units, restockingShop.getReferenceCode(), restockingShop.getName(), restockingShop.getAssignedManager().getUsername(), restockingShop.getAssignedManager().getName(), product.getReferenceCode(), product.getName(), null, null);
        eventPublisher.publishEvent(new RegistryEvent(stockRegistry));

        Registry capacityRegistry = new Registry(EntityType.SHOP, RegistryType.SHOP_USED_CAPACITY, units * product.getCapacity(), restockingShop.getReferenceCode(), restockingShop.getName(), restockingShop.getAssignedManager().getUsername(), restockingShop.getAssignedManager().getName(), product.getReferenceCode(), product.getName(), null, null);
        eventPublisher.publishEvent(new RegistryEvent(capacityRegistry));

        return targetStock;
    }

    @Transactional
    public ShopStock setAssignedStock(Long shopId, Long stockId, boolean state){
        Shop shop = this.findShopHelper(shopId);
        ShopStock targetStock;
        Product product = productService.findProductHelper(stockId);

        if (state) {
            targetStock = this.shopStockService.save(new ShopStock(shop, product, 0));
        } else {
            targetStock = shopStockService.findShopStockHelper(stockId);
            this.shopStockService.deleteById(stockId);

            Registry stockRegistry = new Registry(EntityType.SHOP, RegistryType.SHOP_STOCK, - (double) targetStock.getUnits(), shop.getReferenceCode(), shop.getName(), shop.getAssignedManager().getUsername(), shop.getAssignedManager().getName(), product.getReferenceCode(), product.getName(), null, null);
            eventPublisher.publishEvent(new RegistryEvent(stockRegistry));
        }

        //Send notifications
        ShopEvent shopEvent = new ShopEvent(EventAction.STATUS_CHANGED, String.valueOf(targetStock.getId()), false, null, null);
        eventPublisher.publishEvent(shopEvent);

        return targetStock;
    }

    @Transactional
    public Shop uploadShopImage(Long id, MultipartFile image){
        Shop shop = this.findShopHelper(id);

        ImageInfo newImage = imageService.processImageReplacement(
                shop.getImage(),
                image,
                "shops",
                GlobalDefaults::isDefaultShopImage,
                GlobalDefaults::getDefaultShopImage
        );

        shop.setImage(newImage);
        return shop;
    }

    @Transactional
    public Shop deleteShopImage(Long id){
        Shop shop = this.findShopHelper(id);

        if (!GlobalDefaults.isDefaultShopImage(shop.getImage())) {
            imageService.deleteFile(shop.getImage().getS3Key());
            shop.setImage(GlobalDefaults.getDefaultShopImage());
        }
        return shop;
    }

    // --- METRIC METHODS ---

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