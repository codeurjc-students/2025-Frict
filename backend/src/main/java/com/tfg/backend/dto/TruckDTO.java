package com.tfg.backend.dto;

import com.tfg.backend.model.OrderStatus;
import com.tfg.backend.model.StatusLog;
import com.tfg.backend.model.Truck;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TruckDTO {
    private Long id;
    private String referenceCode;
    private Long shopId;
    private UserDTO assignedDriver;
    private double longitude;
    private double latitude;
    private int activeOrdersToDeliver;

    public TruckDTO() {
    }

    public TruckDTO(Truck t){
        this.id = t.getId();
        this.referenceCode = t.getReferenceCode();
        this.shopId = t.getAssignedShop().getId();
        if (t.getAssignedDriver() != null){
            this.assignedDriver = new UserDTO(t.getAssignedDriver());
        }
        this.longitude = t.getLongitude();
        this.latitude = t.getLatitude();
        this.activeOrdersToDeliver = Math.toIntExact(t.getOrdersToDeliver().stream()
                .filter(order -> {
                    List<StatusLog> logs = order.getHistory();

                    if (logs == null || logs.isEmpty()) {
                        return false;
                    }

                    StatusLog currentLog = logs.getLast();
                    OrderStatus status = currentLog.getStatus();

                    // Only keep those order which status is not final
                    return status != OrderStatus.COMPLETED && status != OrderStatus.CANCELLED;
                })
                .count());
    }
}
