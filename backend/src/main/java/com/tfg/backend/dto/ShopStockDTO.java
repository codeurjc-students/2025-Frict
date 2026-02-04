package com.tfg.backend.dto;

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
    private String productName;
    private String productReferenceCode;
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
        this.productName = s.getProduct().getName();
        this.productReferenceCode = s.getProduct().getReferenceCode();
        this.units = s.getUnits();
        this.active = s.isActive();
    }
}
