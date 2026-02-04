package com.tfg.backend.dto;

import com.tfg.backend.model.Truck;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TruckDTO {
    private Long id;
    private String referenceCode;
    private Long shopId;
    private UserDTO assignedDriver;
    private double longitude;
    private double latitude;

    public TruckDTO() {
    }

    public TruckDTO(Truck t){
        this.id = t.getId();
        this.referenceCode = t.getReferenceCode();
        this.shopId = t.getAssignedShop().getId();
        this.assignedDriver = new UserDTO(t.getAssignedDriver());
        this.longitude = t.getLongitude();
        this.latitude = t.getLatitude();
    }
}
