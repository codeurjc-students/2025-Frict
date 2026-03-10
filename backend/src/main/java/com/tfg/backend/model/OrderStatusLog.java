package com.tfg.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class OrderStatusLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "order_updates",
            joinColumns = @JoinColumn(name = "status_id")
    )
    @OrderBy("date ASC")
    private List<LogEntry> updates = new ArrayList<>();

    // Constructor vacío requerido por JPA
    public OrderStatusLog() {
    }

    // Constructor inteligente que maneja el mensaje por defecto
    public OrderStatusLog(OrderStatus status, String description) {
        this.status = status;

        // Si el front no envía comentario (es nulo o está vacío), usamos el del Enum
        String finalDescription = (description == null || description.trim().isEmpty())
                ? status.getDefaultMessage()
                : description;

        this.updates.add(new LogEntry(finalDescription));
    }

    // If description is empty, then add the current status default description
    public void addUpdate(String description) {
        String finalDescription = (description == null || description.trim().isEmpty())
                ? this.status.getDefaultMessage()
                : description;

        this.updates.add(new LogEntry(finalDescription));
    }
}