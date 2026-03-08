package com.tfg.backend.dto;

import com.tfg.backend.model.OrderStatus;
import com.tfg.backend.model.OrderStatusLog;
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
    private int activeOrdersToDeliver;
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
        this.address = new AddressDTO(t.getAddress());
        this.activeOrdersToDeliver = Math.toIntExact(t.getOrdersToDeliver().stream()
                .filter(order -> {
                    List<OrderStatusLog> logs = order.getHistory();

                    if (logs == null || logs.isEmpty()) {
                        return false;
                    }

                    OrderStatusLog currentLog = logs.getLast();
                    OrderStatus status = currentLog.getStatus();

                    // Only keep those order which status is not final
                    return status != OrderStatus.COMPLETED && status != OrderStatus.CANCELLED;
                })
                .count());
        this.maxOrderCapacity = t.getMaxOrderCapacity();
    }
}
