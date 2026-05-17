package com.tfg.backend.model;

import com.tfg.backend.utils.ReferenceNumberGenerator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "trucks")
public class Truck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String referenceCode;

    @Column(unique = true, nullable = false)
    private String plateNumber;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TruckStatusLog> history = new ArrayList<>();

    //Record the address and exact truck positioning
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private Address address;

    @OneToMany(mappedBy = "assignedTruck")
    private Set<Order> ordersToDeliver = new HashSet<>();

    @Column(nullable = false)
    private int maxOrderCapacity;

    @ManyToOne
    private Shop assignedShop;

    @OneToOne
    @JoinColumn(name = "driver_id")
    private User assignedDriver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_order_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Order selectedOrder;


    public Truck() {
        this.history.add(new TruckStatusLog(TruckStatus.REST, "Camión preparado para el reparto."));
    }

    public Truck(String plateNumber, Address address, int maxOrderCapacity) {
        this.referenceCode = ReferenceNumberGenerator.generateTruckReferenceNumber();
        this.plateNumber = plateNumber;
        this.history.add(new TruckStatusLog(TruckStatus.REST, "Camión preparado para el reparto."));
        this.address = address;
        this.maxOrderCapacity = maxOrderCapacity;
    }

    public TruckStatus getCurrentStatus(){
        return this.getHistory().getLast().getStatus();
    }

    //Adds an update to the current status. It does not change the current order status
    public void addStatusUpdate(String description) {
        this.getHistory().getLast().addUpdate(description);
    }

    //Changes the order status to a new one (it may be more than one status of the same type, admitting incidents and cancellations)
    public void changeTruckStatus(TruckStatus status, String description) {
        this.getHistory().addLast(new TruckStatusLog(status, description)); //New status with at least a log (the first description of a status)
    }
}
