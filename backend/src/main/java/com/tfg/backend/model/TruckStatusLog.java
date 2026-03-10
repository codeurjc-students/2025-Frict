package com.tfg.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class TruckStatusLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TruckStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "truck_updates",
            joinColumns = @JoinColumn(name = "status_id")
    )
    @OrderBy("date ASC")
    private List<LogEntry> updates = new ArrayList<>();

    public TruckStatusLog() {
    }

    public TruckStatusLog(TruckStatus status, String description) {
        this.status = status;

        String finalDescription = (description == null || description.trim().isEmpty())
                ? status.getDefaultMessage()
                : description;

        this.updates.add(new LogEntry(finalDescription));
    }

    public void addUpdate(String description) {
        String finalDescription = (description == null || description.trim().isEmpty())
                ? this.status.getDefaultMessage()
                : description;

        this.updates.add(new LogEntry(finalDescription));
    }
}
