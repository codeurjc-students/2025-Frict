package com.tfg.backend.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ShopStockListDTO {

    private List<ShopStockDTO> stocks;

    public ShopStockListDTO(List<ShopStockDTO> stocks) {
        this.stocks = stocks;
    }
}
