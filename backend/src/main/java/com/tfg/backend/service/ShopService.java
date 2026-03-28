package com.tfg.backend.service;

import com.tfg.backend.dto.AddressDTO;
import com.tfg.backend.dto.ShopDTO;
import com.tfg.backend.model.*;
import com.tfg.backend.repository.ShopRepository;
import com.tfg.backend.utils.GlobalDefaults;
import com.tfg.backend.utils.StatDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ShopService {

    @Autowired private StorageService storageService;
    @Autowired private ShopStockService shopStockService;
    @Autowired private ProductService productService;
    @Autowired private ShopRepository shopRepository;

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
    public Shop createShop(ShopDTO shopDTO){
        AddressDTO dto = shopDTO.getAddress();
        Address address = new Address(dto.getAlias(), dto.getStreet(), dto.getNumber(), dto.getFloor(), dto.getPostalCode(), dto.getCity(), dto.getCountry());
        address.setLatitude(dto.getLatitude());
        address.setLongitude(dto.getLongitude());

        Shop shop = new Shop(shopDTO.getName(), address, shopDTO.getAssignedBudget());
        return shopRepository.save(shop);
    }

    @Transactional
    public Shop updateShop(Long id, ShopDTO shopDTO){
        Shop shop = this.findShopHelper(id);

        shop.setName(shopDTO.getName());
        AddressDTO dto = shopDTO.getAddress();
        Address address = new Address(dto.getAlias(), dto.getStreet(), dto.getNumber(), dto.getFloor(), dto.getPostalCode(), dto.getCity(), dto.getCountry());
        address.setLatitude(dto.getLatitude());
        address.setLongitude(dto.getLongitude());

        shop.setAddress(address);
        shop.setAssignedBudget(shopDTO.getAssignedBudget());

        return shop;
    }

    @Transactional
    public Shop deleteShop(Long id){
        Shop shop = this.findShopHelper(id);

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

        shopRepository.delete(shop);

        if (shop.getImage() != null && !shop.getImage().equals(GlobalDefaults.SHOP_IMAGE)) {
            storageService.deleteFile(shop.getImage().getS3Key());
        }

        return shop;
    }

    @Transactional
    public ShopStock toggleLocalActivation(Long id, boolean state){
        ShopStock stock = shopStockService.findShopStockHelper(id);
        stock.setActive(state);
        return stock;
    }

    @Transactional
    public boolean toggleAllLocalActivations(Long shopId, boolean state){
        List<ShopStock> stocks = this.shopStockService.findAllByShopId(shopId);
        stocks.forEach(s -> s.setActive(state));
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
        return targetStock;
    }

    @Transactional
    public Shop uploadShopImage(Long id, MultipartFile image){
        Shop shop = this.findShopHelper(id);

        if (shop.getImage() != null && !shop.getImage().equals(GlobalDefaults.SHOP_IMAGE)) {
            storageService.deleteFile(shop.getImage().getS3Key());
        }

        if (image != null && !image.isEmpty()){
            try {
                Map<String, String> res = storageService.uploadFile(image, "shops");
                ImageInfo shopImageInfo = new ImageInfo(res.get("url"), res.get("key"), image.getOriginalFilename());
                shop.setImage(shopImageInfo);
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not upload shop image to storage");
            }
        } else {
            shop.setImage(GlobalDefaults.SHOP_IMAGE);
        }
        return shop;
    }

    @Transactional
    public Shop deleteShopImage(Long id){
        Shop shop = this.findShopHelper(id);

        if (!shop.getImage().equals(GlobalDefaults.SHOP_IMAGE)) {
            storageService.deleteFile(shop.getImage().getS3Key());
            shop.setImage(GlobalDefaults.SHOP_IMAGE);
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