package com.tfg.backend.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ShopStockEvent {

    public enum StockAction {
        RESTOCKED,
        LOW_STOCK
    }

    private final StockAction action;

    private final Long shopId;
    private final String shopName;
    private final String shopRefCode;

    private final Long productId;
    private final String productName;
    private final String productRefCode;

    private final int oldUnits;
    private final int newUnits;
    private final int threshold;

    private final String managerUsername;
}
