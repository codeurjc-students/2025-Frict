package com.tfg.backend.model;

import com.tfg.backend.utils.AttributeEncryptor;
import com.tfg.backend.utils.ReferenceNumberGenerator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String referenceCode;

    //The current order status will always be the last element of the history list
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderStatusLog> history = new ArrayList<>(); //Unidirectional relation with OrderStatusLog (as it is only updated from Order)

    @ManyToOne
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @ManyToOne
    private Truck assignedTruck;

    @ManyToOne
    private Shop assignedShop;

    private int estimatedCompletionTime = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private LocalDateTime createdAt;

    @Convert(converter = AttributeEncryptor.class)
    @Column(name = "qr_delivery_token")
    private String qrDeliveryToken;

    //Fields from CartSummaryDTO
    private int totalItems;
    private double subtotalCost;
    private double totalDiscount;
    private double shippingCost;
    private double totalCost;

    private double totalCapacity = 0.0;

    private String cardNumberEnding; //Historic fields from Address and PaymentCard (prevent that, when the user deletes their addresses or cards, the order remains identifiable)

    @ManyToOne
    private Address fullSendingAddress;

    public Order() {
        this.history.add(new OrderStatusLog(OrderStatus.ORDER_MADE, "Pedido recibido correctamente"));
        this.qrDeliveryToken = UUID.randomUUID().toString();
    }

    public Order(User user, List<OrderItem> items, Shop assignedShop, Address address, PaymentCard card) {
        this.referenceCode = ReferenceNumberGenerator.generateOrderReferenceNumber();

        this.history.add(new OrderStatusLog(OrderStatus.ORDER_MADE, "Pedido recibido correctamente"));

        this.qrDeliveryToken = UUID.randomUUID().toString();

        this.user = user;
        for (OrderItem item : items) {
            item.setOrder(this);
            this.items.add(item);
        }

        this.assignedShop = assignedShop;
        this.cardNumberEnding = card.getNumber().substring(card.getNumber().length() - 4);
        this.fullSendingAddress = address;
        this.updateSummaryFields();
    }

    public void updateSummaryFields(){
        int totalItems = 0;
        double subtotal = 0.0;
        double totalDiscount = 0.0;
        double total = 0.0;

        for (OrderItem item : this.getItems()) {
            Product p = item.getProduct();
            double currentPrice = item.getProductPrice();
            double previousPrice = 0.0;
            if(p != null){
                previousPrice = p.getPreviousPrice();
            }

            int quantity = item.getQuantity();
            totalItems += quantity;

            // Subtotal
            double unitSubtotal = (previousPrice > 0) ? previousPrice : currentPrice;
            subtotal += unitSubtotal * quantity;

            // Total
            total += currentPrice * quantity;

            // Discount
            if (previousPrice > currentPrice) {
                totalDiscount += (previousPrice - currentPrice) * quantity;
            }
        }

        double totalCapacity = 0.0;
        for (OrderItem item : this.getItems()) {
            if (item.getProduct() != null) {
                totalCapacity += item.getQuantity() * item.getProduct().getCapacity();
            }
        }

        this.totalItems = totalItems;
        this.subtotalCost = Math.round(subtotal * 100.0) / 100.0;
        this.totalDiscount = Math.round(totalDiscount * 100.0) / 100.0;
        this.shippingCost = (total > 0.0 && total < 50.0) ? 5.0 : 0.0;
        this.totalCost = Math.round(total * 100.0) / 100.0;
        this.totalCapacity = totalCapacity;
    }

    public OrderStatus getCurrentStatus(){
        return this.getHistory().getLast().getStatus();
    }

    //Adds an update to the current status. It does not change the current order status
    public void addStatusUpdate(String description) {
        this.getHistory().getLast().addUpdate(description);
    }

    //Changes the order status to a new one (it may be more than one status of the same type, admitting incidents and cancellations)
    public void changeOrderStatus(OrderStatus status, String description) {
        this.getHistory().addLast(new OrderStatusLog(status, description)); //New status with at least a log (the first description of a status)
    }
}
