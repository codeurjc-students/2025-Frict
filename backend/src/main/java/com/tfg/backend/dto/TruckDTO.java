package com.tfg.backend.dto;

import com.tfg.backend.model.DriverLocation;
import com.tfg.backend.model.Truck;
import com.tfg.backend.model.TruckStatusLog;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TruckDTO {
    private Long id;
    private String referenceCode;
    private String plateNumber;
    private List<TruckStatusLogDTO> history = new ArrayList<>();
    private Long shopId;
    private UserDTO assignedDriver;
    private AddressDTO address;
    private DriverLocation driverLocation;
    private int ordersToDeliver;
    private int maxOrderCapacity;

    public TruckDTO() {
    }

    public TruckDTO(Truck t){
        this.id = t.getId();
        this.referenceCode = t.getReferenceCode();
        this.plateNumber = t.getPlateNumber();
        for (TruckStatusLog l : t.getHistory()) {
            this.history.add(new TruckStatusLogDTO(l));
        }
        if (t.getAssignedShop() != null){
            this.shopId = t.getAssignedShop().getId();
        }
        if (t.getAssignedDriver() != null){
            this.assignedDriver = new UserDTO(t.getAssignedDriver());
        }
        if (t.getAddress() != null){
            this.address = new AddressDTO(t.getAddress());
        }
        this.ordersToDeliver = t.getOrdersToDeliver().size();
        this.maxOrderCapacity = t.getMaxOrderCapacity();
    }
}
