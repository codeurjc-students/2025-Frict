package com.tfg.backend.model;

import com.tfg.backend.utils.ReferenceNumberGenerator;
import jakarta.persistence.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    @Setter(AccessLevel.NONE)
    private String referenceCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private OrderStatus status;

    @ManyToOne
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @ManyToOne
    private Truck assignedTruck;

    private int estimatedCompletionTime = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private LocalDateTime createdAt;

    //Fields from CartSummaryDTO
    private int totalItems;
    private double subtotalCost;
    private double totalDiscount;
    private double shippingCost;
    private double totalCost;

    private String cardNumberEnding; //Historic fields from Address and PaymentCard (prevent that, when the user deletes their addresses or cards, the order remains identifiable)
    private String fullSendingAddress;

    public Order() {
    }

    public Order(User user, List<OrderItem> items, Address address, PaymentCard card) {
        this.referenceCode = ReferenceNumberGenerator.generateOrderReferenceNumber();
        this.status = OrderStatus.ORDER_MADE;
        this.user = user;
        for (OrderItem item : items) {
            item.setOrder(this);
            this.items.add(item);
        }
        this.cardNumberEnding = card.getNumber().substring(card.getNumber().length() - 4);
        this.fullSendingAddress = address.toString();
        this.updateSummaryFields();
    }

    public void updateSummaryFields(){
        int totalItems = 0;
        double subtotal = 0.0;
        double totalDiscount = 0.0;
        double total = 0.0;

        for (OrderItem item : this.getItems()) {
            Product p = item.getProduct();
            int quantity = item.getQuantity();
            totalItems += quantity;

            double currentPrice = p.getCurrentPrice();
            double previousPrice = p.getPreviousPrice();

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

        this.totalItems = totalItems;
        this.subtotalCost = Math.round(subtotal * 100.0) / 100.0;
        this.totalDiscount = Math.round(totalDiscount * 100.0) / 100.0;
        this.shippingCost = (total > 50.0) ? 0.0 : 5.0;
        this.totalCost = Math.round(total * 100.0) / 100.0;
    }
}
