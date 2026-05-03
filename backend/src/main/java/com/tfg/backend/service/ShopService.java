package com.tfg.backend.service;

import com.tfg.backend.dto.AddressDTO;
import com.tfg.backend.dto.ShopDTO;
import com.tfg.backend.model.*;
import com.tfg.backend.notification.EntityType;
import com.tfg.backend.notification.EventAction;
import com.tfg.backend.notification.ShopEvent;
import com.tfg.backend.notification.TruckEvent;
import com.tfg.backend.registry.Registry;
import com.tfg.backend.registry.RegistryType;
import com.tfg.backend.repository.ShopRepository;
import com.tfg.backend.utils.GlobalDefaults;
import com.tfg.backend.utils.StatDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ShopService {

    private final ImageService imageService;
    private final ShopStockService shopStockService;
    private final ProductService productService;
    private final ShopRepository shopRepository;
    private final ApplicationEventPublisher eventPublisher;

    // --- READ-ONLY METHODS ---

    public List<Shop> findAll() { return shopRepository.findAll(); }
    public Page<Shop> findAll(Pageable pageInfo) { return shopRepository.findAll(pageInfo); }
    public Page<Shop> findAllByAssignedManagerId(Long userId, Pageable pageInfo) { return shopRepository.findAllByAssignedManagerId(userId, pageInfo); }
    public Optional<Shop> findById(Long id) { return shopRepository.findById(id); }

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

        targetStock.setUnits(targetStock.getUnits() + units);
        restockingShop.setAssignedBudget(restockingShop.getAssignedBudget() - supplyCost);

        //Send notifications
        ShopEvent shopEvent = new ShopEvent(EventAction.STATUS_CHANGED, String.valueOf(targetStock.getId()), true, null, null);
        eventPublisher.publishEvent(shopEvent);
        return targetStock;
    }

    @Transactional
    public ShopStock setAssignedStock(Long shopId, Long stockId, boolean state){
        Shop shop = this.findShopHelper(shopId);
        ShopStock targetStock;

        if (state) {
            Product product = productService.findProductHelper(stockId);
            targetStock = this.shopStockService.save(new ShopStock(shop, product, 0));
        } else {
            targetStock = shopStockService.findShopStockHelper(stockId);
            this.shopStockService.deleteById(stockId);
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