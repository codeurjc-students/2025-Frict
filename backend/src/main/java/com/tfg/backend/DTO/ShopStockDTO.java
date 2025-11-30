package com.tfg.backend.DTO;

import com.tfg.backend.model.ShopStock;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShopStockDTO {
    private Long id;
    private Long shopId;
    private String shopName;
    private String shopAddress;
    private Long productId;
    private int units;
    private boolean active;

    public ShopStockDTO() {
    }

    public ShopStockDTO(ShopStock s) {
        this.id = s.getId();
        this.shopId = s.getShop().getId();
        this.shopName = s.getShop().getName();
        this.shopAddress = s.getShop().getAddress().toString();
        this.productId = s.getProduct().getId();
        this.units = s.getStock();
        this.active = s.isActive();
    }
}
