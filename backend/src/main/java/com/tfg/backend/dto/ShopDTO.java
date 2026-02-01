package com.tfg.backend.dto;

import com.tfg.backend.model.ImageInfo;
import com.tfg.backend.model.Shop;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShopDTO {
    private Long id;
    private String referenceCode;
    private String name;
    private AddressDTO address;
    private ImageInfo imageInfo;
    private int totalAvailableProducts;
    private int totalAssignedTrucks;
    private double longitude;
    private double latitude;
    private UserDTO assignedManager;

    public ShopDTO() {
    }

    public ShopDTO (Shop s){
        this.id = s.getId();
        this.referenceCode = s.getReferenceCode();
        this.name = s.getName();
        this.address = new AddressDTO(s.getAddress());
        this.imageInfo = s.getImage();
        this.totalAvailableProducts = s.getAvailableProducts().size();
        this.totalAssignedTrucks = s.getAssignedTrucks().size();
        this.longitude = s.getLongitude();
        this.latitude = s.getLatitude();
        if (s.getAssignedManager() != null){
            this.assignedManager = new UserDTO(s.getAssignedManager());
        }
    }
}
