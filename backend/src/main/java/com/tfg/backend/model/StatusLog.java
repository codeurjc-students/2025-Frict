package com.tfg.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class StatusLog {

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

    public StatusLog() {
    }

    public StatusLog(OrderStatus status, String description) {
        this.status = status;
        this.updates.add(new LogEntry(description));
    }
}
